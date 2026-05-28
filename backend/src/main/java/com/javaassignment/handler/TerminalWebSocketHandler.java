package com.javaassignment.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

@Component
public class TerminalWebSocketHandler extends TextWebSocketHandler {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Value("${compiler.timeout-seconds:30}")
    private int timeoutSeconds;

    /** Per-session mutable state. */
    private static class Session {
        volatile Process  process;
        volatile OutputStream stdin;
        volatile Path     tempDir;
        volatile boolean  alive = true;
    }

    private final ConcurrentHashMap<String, Session> sessions = new ConcurrentHashMap<>();

    // ── Incoming messages ─────────────────────────────────────────────────────

    @Override
    protected void handleTextMessage(WebSocketSession ws, TextMessage raw) {
        try {
            JsonNode n    = MAPPER.readTree(raw.getPayload());
            String   type = n.get("type").asText();
            switch (type) {
                case "run"   -> handleRun  (ws, n.get("code").asText());
                case "input" -> handleInput(ws.getId(), n.get("data").asText());
                case "kill"  -> killSession(ws.getId(), ws, "Killed by user.");
            }
        } catch (Exception e) {
            send(ws, Map.of("type", "error", "data", e.getMessage()));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession ws, CloseStatus status) {
        killSession(ws.getId(), null, null);
    }

    // ── Run ───────────────────────────────────────────────────────────────────

    private void handleRun(WebSocketSession ws, String code) {
        killSession(ws.getId(), null, null);   // kill any previous session

        Thread.ofVirtual().start(() -> {
            Path tempDir = null;
            try {
                tempDir = Files.createTempDirectory("jac_ws_");
                final Path dir = tempDir;

                String className = extractClassName(code);
                Files.writeString(dir.resolve(className + ".java"), code, StandardCharsets.UTF_8);

                // ── 1. Compile ────────────────────────────────────────────────
                long t0 = System.currentTimeMillis();
                ProcessBuilder javac = new ProcessBuilder("javac", className + ".java");
                javac.directory(dir.toFile());
                javac.redirectErrorStream(true);
                Process cp = javac.start();
                String  co = drain(cp.getInputStream());
                boolean ok = cp.waitFor(10, TimeUnit.SECONDS);
                long compileMs = System.currentTimeMillis() - t0;

                if (!ok || cp.exitValue() != 0) {
                    send(ws, Map.of("type", "compile_error",
                            "data", co.isBlank() ? "Compile timed out." : co));
                    return;
                }
                send(ws, Map.of("type", "compiled", "ms", compileMs));

                // ── 2. Execute ────────────────────────────────────────────────
                ProcessBuilder java = new ProcessBuilder(
                        "java", "-cp", dir.toString(), "-Dfile.encoding=UTF-8", className);
                java.directory(dir.toFile());
                Process proc = java.start();

                Session sess = new Session();
                sess.process = proc;
                sess.stdin   = proc.getOutputStream();
                sess.tempDir = dir;
                sessions.put(ws.getId(), sess);

                long t1 = System.currentTimeMillis();

                // Stream stdout
                Thread.ofVirtual().start(() -> {
                    try (InputStream is = proc.getInputStream()) {
                        byte[] buf = new byte[256];
                        int    n;
                        while (sess.alive && (n = is.read(buf)) != -1) {
                            send(ws, Map.of("type", "output",
                                    "data", new String(buf, 0, n, StandardCharsets.UTF_8)));
                        }
                    } catch (IOException ignored) {}
                });

                // Stream stderr
                Thread.ofVirtual().start(() -> {
                    try (InputStream is = proc.getErrorStream()) {
                        byte[] buf = new byte[256];
                        int    n;
                        while (sess.alive && (n = is.read(buf)) != -1) {
                            send(ws, Map.of("type", "stderr",
                                    "data", new String(buf, 0, n, StandardCharsets.UTF_8)));
                        }
                    } catch (IOException ignored) {}
                });

                // Wait for exit
                boolean done   = proc.waitFor(timeoutSeconds, TimeUnit.SECONDS);
                long    execMs = System.currentTimeMillis() - t1;
                sess.alive = false;

                if (!done) {
                    proc.destroyForcibly();
                    send(ws, Map.of("type", "exit", "code", -1,
                            "timeout", true, "execMs", execMs));
                } else {
                    send(ws, Map.of("type", "exit", "code", proc.exitValue(),
                            "timeout", false, "execMs", execMs));
                }

            } catch (Exception e) {
                send(ws, Map.of("type", "error", "data", "Server error: " + e.getMessage()));
            } finally {
                sessions.remove(ws.getId());
                if (tempDir != null) deleteDir(tempDir);
            }
        });
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    private void handleInput(String sessionId, String data) {
        Session sess = sessions.get(sessionId);
        if (sess == null || sess.stdin == null) return;
        try {
            sess.stdin.write(data.getBytes(StandardCharsets.UTF_8));
            sess.stdin.flush();
        } catch (IOException ignored) {}
    }

    // ── Kill ──────────────────────────────────────────────────────────────────

    private void killSession(String sessionId, WebSocketSession ws, String msg) {
        Session sess = sessions.remove(sessionId);
        if (sess == null) return;
        sess.alive = false;
        if (sess.process != null) sess.process.destroyForcibly();
        if (sess.tempDir  != null) deleteDir(sess.tempDir);
        if (ws != null && msg != null)
            send(ws, Map.of("type", "exit", "code", -1, "timeout", false,
                    "execMs", 0L, "killed", true));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void send(WebSocketSession ws, Object payload) {
        if (!ws.isOpen()) return;
        try {
            synchronized (ws) {
                ws.sendMessage(new TextMessage(MAPPER.writeValueAsString(payload)));
            }
        } catch (IOException ignored) {}
    }

    private String extractClassName(String code) {
        Matcher m = Pattern.compile("public\\s+class\\s+(\\w+)").matcher(code);
        if (m.find()) return m.group(1);
        m = Pattern.compile("class\\s+(\\w+)").matcher(code);
        String last = "Main";
        while (m.find()) last = m.group(1);
        return last;
    }

    private String drain(InputStream is) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        int n;
        while ((n = is.read(chunk)) != -1) buf.write(chunk, 0, n);
        return buf.toString(StandardCharsets.UTF_8);
    }

    private void deleteDir(Path dir) {
        try {
            Files.walk(dir).sorted(Comparator.reverseOrder())
                    .forEach(p -> { try { Files.deleteIfExists(p); } catch (IOException ignored) {} });
        } catch (IOException ignored) {}
    }
}

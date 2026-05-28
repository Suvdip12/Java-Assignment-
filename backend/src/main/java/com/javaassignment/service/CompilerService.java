package com.javaassignment.service;

import com.javaassignment.dto.CompileRequest;
import com.javaassignment.dto.CompileResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class CompilerService {

    @Value("${compiler.timeout-seconds:10}")
    private int timeoutSeconds;

    @Value("${compiler.max-output-bytes:102400}")
    private int maxOutputBytes;

    public CompileResponse compile(CompileRequest request) {
        CompileResponse response = new CompileResponse();
        Path tempDir = null;

        try {
            tempDir = Files.createTempDirectory("jac_");
            String code      = request.getCode();
            String className = extractMainClassName(code);

            Files.writeString(tempDir.resolve(className + ".java"), code, StandardCharsets.UTF_8);

            // ── 1. COMPILE ────────────────────────────────────────────────────
            long compileStart = System.currentTimeMillis();

            ProcessBuilder javac = new ProcessBuilder("javac", className + ".java");
            javac.directory(tempDir.toFile());
            javac.redirectErrorStream(true);

            Process compileProc = javac.start();
            String  compileOut  = drain(compileProc.getInputStream());
            boolean compiled    = compileProc.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            response.setCompileTimeMs(System.currentTimeMillis() - compileStart);

            if (!compiled || compileProc.exitValue() != 0) {
                response.setSuccess(false);
                response.setCompileError(compileOut.isEmpty() ? "Compile timed out." : compileOut);
                return response;
            }

            // ── 2. PREPARE STDIN ─────────────────────────────────────────────
            // Normalise line-endings so Scanner.nextLine() / nextDouble() works
            String rawStdin = request.getStdin();
            final String stdinData;
            if (rawStdin != null && !rawStdin.isBlank()) {
                String s = rawStdin.replace("\r\n", "\n").replace("\r", "\n").trim();
                stdinData = s + "\n";           // trailing newline = EOF signal for Scanner
            } else {
                stdinData = null;
            }

            // ── 3. EXECUTE ────────────────────────────────────────────────────
            long execStart = System.currentTimeMillis();

            ProcessBuilder java = new ProcessBuilder(
                    "java",
                    "-cp", tempDir.toString(),
                    "-Dfile.encoding=UTF-8",
                    "-Dstdout.encoding=UTF-8",
                    className);
            java.directory(tempDir.toFile());

            Process runProc = java.start();

            // Three parallel threads prevent stdout/stderr buffer-deadlock.
            // tIn writes stdin and closes the stream (EOF), then stdout/stderr
            // are drained concurrently while we wait for the process.
            StringBuilder outBuf = new StringBuilder();
            StringBuilder errBuf = new StringBuilder();

            Thread tOut = new Thread(() -> {
                try { outBuf.append(drain(runProc.getInputStream())); }
                catch (IOException ignored) {}
            });
            Thread tErr = new Thread(() -> {
                try { errBuf.append(drain(runProc.getErrorStream())); }
                catch (IOException ignored) {}
            });
            Thread tIn = new Thread(() -> {
                try (OutputStream os = runProc.getOutputStream()) {
                    if (stdinData != null) {
                        os.write(stdinData.getBytes(StandardCharsets.UTF_8));
                        os.flush();
                    }
                    // closing stream sends EOF → Scanner/BufferedReader stops blocking
                } catch (IOException ignored) {}
            });

            tOut.start();
            tErr.start();
            tIn.start();

            // Wait for stdin to be fully written before timing the execution
            tIn.join(5000);

            boolean done = runProc.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            tOut.join(5000);
            tErr.join(2000);
            response.setExecutionTimeMs(System.currentTimeMillis() - execStart);

            if (!done) {
                runProc.destroyForcibly();
                response.setSuccess(false);
                response.setRuntimeError("Timed out after " + timeoutSeconds + "s – check for infinite loops or missing input.");
                return response;
            }

            String stdout = outBuf.toString();
            String stderr = errBuf.toString();

            if (runProc.exitValue() != 0 && !stderr.isBlank()) {
                response.setSuccess(false);
                response.setOutput(stdout.isBlank() ? null : stdout);
                response.setRuntimeError(stderr);
                return response;
            }

            response.setSuccess(true);
            response.setOutput(stdout.isBlank() ? "(no output)" : stdout);

        } catch (Exception e) {
            response.setSuccess(false);
            response.setRuntimeError("Server error: " + e.getMessage());
        } finally {
            if (tempDir != null) deleteDir(tempDir);
        }

        return response;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String extractMainClassName(String code) {
        Matcher m = Pattern.compile("public\\s+class\\s+(\\w+)").matcher(code);
        if (m.find()) return m.group(1);

        m = Pattern.compile("class\\s+(\\w+)[^{]*\\{(?:[^{}]|\\{[^{}]*\\})*public\\s+static\\s+void\\s+main",
                Pattern.DOTALL).matcher(code);
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

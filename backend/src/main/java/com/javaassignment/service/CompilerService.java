package com.javaassignment.service;

import com.javaassignment.dto.CompileRequest;
import com.javaassignment.dto.CompileResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
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
            tempDir = Files.createTempDirectory("java_compile_");
            String code = request.getCode();
            String className = extractMainClassName(code);

            Files.writeString(tempDir.resolve(className + ".java"), code);

            // ── Compile ──────────────────────────────────────────────────────
            long compileStart = System.currentTimeMillis();
            ProcessBuilder compileBuilder = new ProcessBuilder("javac", className + ".java");
            compileBuilder.directory(tempDir.toFile());
            compileBuilder.redirectErrorStream(true);

            Process compileProcess = compileBuilder.start();
            String compileOutput = drain(compileProcess.getInputStream());
            boolean compiled = compileProcess.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            response.setCompileTimeMs(System.currentTimeMillis() - compileStart);

            if (!compiled || compileProcess.exitValue() != 0) {
                response.setSuccess(false);
                response.setCompileError(compileOutput.isEmpty() ? "Compilation timed out" : compileOutput);
                return response;
            }

            // ── Write stdin to a temp file (most reliable approach) ──────────
            ProcessBuilder runBuilder = new ProcessBuilder("java", "-cp", tempDir.toString(), className);
            runBuilder.directory(tempDir.toFile());

            String stdin = request.getStdin();
            if (stdin != null && !stdin.isBlank()) {
                if (!stdin.endsWith("\n")) stdin += "\n";
                Path stdinFile = tempDir.resolve("stdin.txt");
                Files.writeString(stdinFile, stdin);
                runBuilder.redirectInput(stdinFile.toFile());   // process reads stdin from file
            }

            // ── Execute ───────────────────────────────────────────────────────
            long execStart = System.currentTimeMillis();
            Process runProcess = runBuilder.start();

            // Close stdin pipe if no input file was set
            if (stdin == null || stdin.isBlank()) {
                runProcess.getOutputStream().close();
            }

            // Read stdout + stderr in parallel threads to prevent buffer deadlock
            StringBuilder stdoutBuf = new StringBuilder();
            StringBuilder stderrBuf = new StringBuilder();
            Thread t1 = new Thread(() -> { try { stdoutBuf.append(drain(runProcess.getInputStream()));  } catch (IOException ignored) {} });
            Thread t2 = new Thread(() -> { try { stderrBuf.append(drain(runProcess.getErrorStream())); } catch (IOException ignored) {} });
            t1.start(); t2.start();

            boolean finished = runProcess.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            t1.join(3000); t2.join(3000);
            response.setExecutionTimeMs(System.currentTimeMillis() - execStart);

            if (!finished) {
                runProcess.destroyForcibly();
                response.setSuccess(false);
                response.setRuntimeError("Execution timed out after " + timeoutSeconds + " seconds.");
                return response;
            }

            String stdout = stdoutBuf.toString();
            String stderr = stderrBuf.toString();

            if (runProcess.exitValue() != 0 && !stderr.isBlank()) {
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
            if (tempDir != null) deleteDirectory(tempDir);
        }

        return response;
    }

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
        int read;
        while ((read = is.read(chunk)) != -1) buf.write(chunk, 0, read);
        return buf.toString();
    }

    private void deleteDirectory(Path dir) {
        try {
            Files.walk(dir).sorted(Comparator.reverseOrder())
                .forEach(p -> { try { Files.deleteIfExists(p); } catch (IOException ignored) {} });
        } catch (IOException ignored) {}
    }
}

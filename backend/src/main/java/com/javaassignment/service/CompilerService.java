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

            Path sourceFile = tempDir.resolve(className + ".java");
            Files.writeString(sourceFile, code);

            // ── Compile ──────────────────────────────────────────────────────
            long compileStart = System.currentTimeMillis();
            ProcessBuilder compileBuilder = new ProcessBuilder("javac", className + ".java");
            compileBuilder.directory(tempDir.toFile());
            compileBuilder.redirectErrorStream(true);

            Process compileProcess = compileBuilder.start();
            String compileOutput = readStreamFully(compileProcess.getInputStream(), maxOutputBytes);
            boolean compiled = compileProcess.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            response.setCompileTimeMs(System.currentTimeMillis() - compileStart);

            if (!compiled || compileProcess.exitValue() != 0) {
                response.setSuccess(false);
                response.setCompileError(compileOutput.isEmpty() ? "Compilation timed out" : compileOutput);
                return response;
            }

            // ── Execute ───────────────────────────────────────────────────────
            long execStart = System.currentTimeMillis();
            ProcessBuilder runBuilder = new ProcessBuilder("java", "-cp", tempDir.toString(), className);
            runBuilder.directory(tempDir.toFile());

            Process runProcess = runBuilder.start();

            // Read stdout and stderr in separate threads to prevent buffer deadlock
            StringBuilder stdoutBuf = new StringBuilder();
            StringBuilder stderrBuf = new StringBuilder();

            Thread stdoutThread = new Thread(() -> {
                try { stdoutBuf.append(readStreamFully(runProcess.getInputStream(), maxOutputBytes)); }
                catch (IOException ignored) {}
            });
            Thread stderrThread = new Thread(() -> {
                try { stderrBuf.append(readStreamFully(runProcess.getErrorStream(), maxOutputBytes)); }
                catch (IOException ignored) {}
            });
            stdoutThread.start();
            stderrThread.start();

            // Write stdin after readers are started
            try (OutputStream os = runProcess.getOutputStream()) {
                String stdin = request.getStdin();
                if (stdin != null && !stdin.isBlank()) {
                    // Ensure each line ends with newline so Scanner.nextLine() / nextDouble() works
                    if (!stdin.endsWith("\n")) stdin = stdin + "\n";
                    os.write(stdin.getBytes());
                    os.flush();
                }
            } catch (IOException ignored) {}

            boolean finished = runProcess.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            stdoutThread.join(3000);
            stderrThread.join(3000);
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
        Pattern publicClass = Pattern.compile("public\\s+class\\s+(\\w+)");
        Matcher m = publicClass.matcher(code);
        if (m.find()) return m.group(1);

        Pattern mainMethod = Pattern.compile(
            "class\\s+(\\w+)[^{]*\\{(?:[^{}]|\\{[^{}]*\\})*public\\s+static\\s+void\\s+main",
            Pattern.DOTALL);
        m = mainMethod.matcher(code);
        if (m.find()) return m.group(1);

        Pattern anyClass = Pattern.compile("class\\s+(\\w+)");
        m = anyClass.matcher(code);
        String last = "Main";
        while (m.find()) last = m.group(1);
        return last;
    }

    private String readStreamFully(InputStream is, int maxBytes) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        byte[] chunk = new byte[4096];
        int read;
        int total = 0;
        while (total < maxBytes && (read = is.read(chunk)) != -1) {
            int take = Math.min(read, maxBytes - total);
            buf.write(chunk, 0, take);
            total += take;
        }
        return buf.toString();
    }

    private void deleteDirectory(Path dir) {
        try {
            Files.walk(dir)
                .sorted(Comparator.reverseOrder())
                .forEach(p -> { try { Files.deleteIfExists(p); } catch (IOException ignored) {} });
        } catch (IOException ignored) {}
    }
}

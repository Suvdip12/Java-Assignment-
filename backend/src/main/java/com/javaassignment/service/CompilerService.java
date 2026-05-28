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

            // Compile
            long compileStart = System.currentTimeMillis();
            ProcessBuilder compileBuilder = new ProcessBuilder("javac", className + ".java");
            compileBuilder.directory(tempDir.toFile());
            compileBuilder.redirectErrorStream(true);

            Process compileProcess = compileBuilder.start();
            String compileOutput = readStream(compileProcess.getInputStream(), maxOutputBytes);
            boolean compiled = compileProcess.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            long compileTime = System.currentTimeMillis() - compileStart;
            response.setCompileTimeMs(compileTime);

            if (!compiled || compileProcess.exitValue() != 0) {
                response.setSuccess(false);
                response.setCompileError(compileOutput.isEmpty() ? "Compilation timed out" : compileOutput);
                return response;
            }

            // Execute
            long execStart = System.currentTimeMillis();
            ProcessBuilder runBuilder = new ProcessBuilder("java", "-cp", tempDir.toString(), className);
            runBuilder.directory(tempDir.toFile());

            Process runProcess = runBuilder.start();

            // Write stdin
            if (request.getStdin() != null && !request.getStdin().isBlank()) {
                try (OutputStream os = runProcess.getOutputStream()) {
                    os.write(request.getStdin().getBytes());
                    os.flush();
                }
            } else {
                runProcess.getOutputStream().close();
            }

            String stdout = readStream(runProcess.getInputStream(), maxOutputBytes);
            String stderr = readStream(runProcess.getErrorStream(), maxOutputBytes);
            boolean finished = runProcess.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            long execTime = System.currentTimeMillis() - execStart;
            response.setExecutionTimeMs(execTime);

            if (!finished) {
                runProcess.destroyForcibly();
                response.setSuccess(false);
                response.setRuntimeError("Execution timed out after " + timeoutSeconds + " seconds.");
                return response;
            }

            if (runProcess.exitValue() != 0 && !stderr.isBlank()) {
                response.setSuccess(false);
                response.setOutput(stdout);
                response.setRuntimeError(stderr);
                return response;
            }

            response.setSuccess(true);
            response.setOutput(stdout.isBlank() ? "(no output)" : stdout);

        } catch (Exception e) {
            response.setSuccess(false);
            response.setRuntimeError("Server error: " + e.getMessage());
        } finally {
            if (tempDir != null) {
                deleteDirectory(tempDir);
            }
        }

        return response;
    }

    private String extractMainClassName(String code) {
        // Try public class first
        Pattern publicClass = Pattern.compile("public\\s+class\\s+(\\w+)");
        Matcher m = publicClass.matcher(code);
        if (m.find()) return m.group(1);

        // Find class with main method
        Pattern mainMethod = Pattern.compile(
            "class\\s+(\\w+)[^{]*\\{(?:[^{}]|\\{[^{}]*\\})*public\\s+static\\s+void\\s+main",
            Pattern.DOTALL);
        m = mainMethod.matcher(code);
        if (m.find()) return m.group(1);

        // Fallback: last class defined
        Pattern anyClass = Pattern.compile("class\\s+(\\w+)");
        m = anyClass.matcher(code);
        String last = "Main";
        while (m.find()) last = m.group(1);
        return last;
    }

    private String readStream(InputStream is, int maxBytes) throws IOException {
        byte[] buffer = new byte[maxBytes];
        int totalRead = 0;
        int read;
        while (totalRead < maxBytes && (read = is.read(buffer, totalRead, maxBytes - totalRead)) != -1) {
            totalRead += read;
        }
        return new String(buffer, 0, totalRead);
    }

    private void deleteDirectory(Path dir) {
        try {
            Files.walk(dir)
                .sorted(Comparator.reverseOrder())
                .forEach(p -> {
                    try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                });
        } catch (IOException ignored) {}
    }
}

package com.javaassignment.dto;

public class CompileResponse {
    private boolean success;
    private String output;
    private String compileError;
    private String runtimeError;
    private long compileTimeMs;
    private long executionTimeMs;

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getOutput() { return output; }
    public void setOutput(String output) { this.output = output; }

    public String getCompileError() { return compileError; }
    public void setCompileError(String compileError) { this.compileError = compileError; }

    public String getRuntimeError() { return runtimeError; }
    public void setRuntimeError(String runtimeError) { this.runtimeError = runtimeError; }

    public long getCompileTimeMs() { return compileTimeMs; }
    public void setCompileTimeMs(long compileTimeMs) { this.compileTimeMs = compileTimeMs; }

    public long getExecutionTimeMs() { return executionTimeMs; }
    public void setExecutionTimeMs(long executionTimeMs) { this.executionTimeMs = executionTimeMs; }
}

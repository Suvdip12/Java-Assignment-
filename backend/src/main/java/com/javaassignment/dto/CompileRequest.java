package com.javaassignment.dto;

public class CompileRequest {
    private String code;
    private String stdin;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getStdin() { return stdin; }
    public void setStdin(String stdin) { this.stdin = stdin; }
}

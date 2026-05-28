package com.javaassignment.controller;

import com.javaassignment.dto.CompileRequest;
import com.javaassignment.dto.CompileResponse;
import com.javaassignment.service.CompilerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class CompilerController {

    private final CompilerService compilerService;

    public CompilerController(CompilerService compilerService) {
        this.compilerService = compilerService;
    }

    @PostMapping("/compile")
    public ResponseEntity<CompileResponse> compile(@RequestBody CompileRequest request) {
        if (request.getCode() == null || request.getCode().isBlank()) {
            CompileResponse err = new CompileResponse();
            err.setSuccess(false);
            err.setCompileError("No code provided.");
            return ResponseEntity.badRequest().body(err);
        }
        return ResponseEntity.ok(compilerService.compile(request));
    }
}

package com.javaassignment.controller;

import com.javaassignment.model.Problem;
import com.javaassignment.service.ProblemService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/problems")
@CrossOrigin(origins = "*")
public class ProblemController {

    private final ProblemService problemService;

    public ProblemController(ProblemService problemService) {
        this.problemService = problemService;
    }

    @GetMapping
    public List<Problem> getAll() {
        return problemService.getAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Problem> getById(@PathVariable int id) {
        return problemService.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Problem> add(@RequestBody Problem problem) {
        return ResponseEntity.ok(problemService.add(problem));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Problem> update(@PathVariable int id, @RequestBody Problem problem) {
        return problemService.update(id, problem)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/reset")
    public ResponseEntity<Map<String, Object>> reset(@PathVariable int id) {
        boolean ok = problemService.resetCode(id);
        if (!ok) return ResponseEntity.notFound().build();
        Problem p = problemService.findById(id).orElseThrow();
        return ResponseEntity.ok(Map.of("success", true, "code", p.getCode()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable int id) {
        return problemService.delete(id)
            ? ResponseEntity.noContent().build()
            : ResponseEntity.notFound().build();
    }
}

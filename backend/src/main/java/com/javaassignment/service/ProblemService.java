package com.javaassignment.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.javaassignment.data.ProblemData;
import com.javaassignment.model.Problem;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ProblemService {

    private static final Path DATA_FILE = Paths.get("data/problems.json");
    private final ObjectMapper mapper = new ObjectMapper();
    private List<Problem> problems = new ArrayList<>();
    private final AtomicInteger idCounter = new AtomicInteger();

    @PostConstruct
    public void init() {
        if (Files.exists(DATA_FILE)) {
            try {
                problems = mapper.readValue(DATA_FILE.toFile(),
                    new TypeReference<List<Problem>>() {});
                int maxId = problems.stream().mapToInt(Problem::getId).max().orElse(0);
                idCounter.set(maxId);
                return;
            } catch (IOException e) {
                System.err.println("Could not load saved problems, using defaults: " + e.getMessage());
            }
        }
        problems = new ArrayList<>(ProblemData.getDefaultProblems());
        idCounter.set(problems.stream().mapToInt(Problem::getId).max().orElse(0));
        save();
    }

    public List<Problem> getAll() {
        return problems;
    }

    public Optional<Problem> findById(int id) {
        return problems.stream().filter(p -> p.getId() == id).findFirst();
    }

    public Problem add(Problem p) {
        p.setId(idCounter.incrementAndGet());
        p.setOriginalCode(p.getCode());
        problems.add(p);
        save();
        return p;
    }

    public Optional<Problem> update(int id, Problem updated) {
        return findById(id).map(p -> {
            if (updated.getCode() != null) p.setCode(updated.getCode());
            if (updated.getTitle() != null) p.setTitle(updated.getTitle());
            if (updated.getDescription() != null) p.setDescription(updated.getDescription());
            if (updated.getDefaultInput() != null) p.setDefaultInput(updated.getDefaultInput());
            if (updated.getInputType() != null) p.setInputType(updated.getInputType());
            save();
            return p;
        });
    }

    public boolean resetCode(int id) {
        return findById(id).map(p -> {
            p.setCode(p.getOriginalCode());
            save();
            return true;
        }).orElse(false);
    }

    public boolean delete(int id) {
        boolean removed = problems.removeIf(p -> p.getId() == id);
        if (removed) save();
        return removed;
    }

    private void save() {
        try {
            File dir = DATA_FILE.getParent().toFile();
            if (!dir.exists()) dir.mkdirs();
            mapper.writerWithDefaultPrettyPrinter().writeValue(DATA_FILE.toFile(), problems);
        } catch (IOException e) {
            System.err.println("Failed to save problems: " + e.getMessage());
        }
    }
}

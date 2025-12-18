package com.devpro.problem_service.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.devpro.problem_service.dto.ProblemRequest;
import com.devpro.problem_service.model.Problem;
import com.devpro.problem_service.repository.ProblemRepository;

@Service
public class ProblemService {

    private final ProblemRepository repository;

    public ProblemService(ProblemRepository repository) {
        this.repository = repository;
    }

 // CREATE
    public Problem create(ProblemRequest request) {
        Problem p = new Problem();
        map(request, p);
        return repository.save(p);
    }
    
    // READ ALL
    public List<Problem> getAll() {
        return repository.findAll();
    }
    
    // READ BY ID
    public Problem getById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Problem not found"));
    }

//    // UPDATE
//    public Problem update(UUID id, ProblemRequest request) {
//        Problem p = getById(id);
//        map(request, p);
//        return repository.save(p);
//    }
//
//    // DELETE (soft delete)
//    public void delete(UUID id) {
//        Problem p = getById(id);
//        p.setIsActive(false);
//        repository.save(p);
//    }
//
//    private void map(ProblemRequest r, Problem p) {
//        p.setTitle(r.getTitle());
//        p.setSlug(r.getSlug());
//        p.setDescription(r.getDescription());
//        p.setDifficulty(r.getDifficulty());
//        p.setCategory(r.getCategory());
//
//        if (r.getIsActive() != null) {
//            p.setIsActive(r.getIsActive());
//        }
//    }
}

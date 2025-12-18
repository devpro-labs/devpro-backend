package com.devpro.problem_service.service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.devpro.problem_service.dto.TestCaseRequest;
import com.devpro.problem_service.model.StorageType;
import com.devpro.problem_service.model.TestCase;
import com.devpro.problem_service.repository.TestCaseRepository;

@Service
public class TestCaseService {

    private static final int DB_LIMIT_KB = 100;

    private final TestCaseRepository repository;
    private final FileStorageService fileStorageService;

    public TestCaseService(
            TestCaseRepository repository,
            FileStorageService fileStorageService) {
        this.repository = repository;
        this.fileStorageService = fileStorageService;
    }

    // CREATE
    public TestCase create(TestCaseRequest request) {

        int sizeKb = calculateSize(request);

        TestCase tc = new TestCase();
        tc.setProblemId(request.getProblemId());
        tc.setExpectedStatus(request.getExpectedStatus());
        tc.setIsHidden(request.getIsHidden());
        tc.setWeight(request.getWeight());
        tc.setSizeKb(sizeKb);

        if (sizeKb <= DB_LIMIT_KB) {
            tc.setStorageType(StorageType.DB);
            tc.setInputJson(request.getInput());
            tc.setExpectedOutputJson(request.getExpectedOutput());
        } else {
            tc.setStorageType(StorageType.FILE);
            tc.setInputFileUrl(
                    fileStorageService.upload(request.getInput(), "input"));
            tc.setExpectedOutputFileUrl(
                    fileStorageService.upload(request.getExpectedOutput(), "expected"));
        }

        return repository.save(tc);
    }

    // READ BY ID
    public TestCase getById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Test case not found"));
    }

    // READ BY PROBLEM
    public List<TestCase> getByProblem(UUID problemId) {
        return repository.findByProblemId(problemId);
    }

    // UPDATE
    public TestCase update(UUID id, TestCaseRequest request) {

        TestCase tc = getById(id);

        // simplest approach: recreate storage if content changed
        repository.delete(tc);
        return create(request);
    }

    // DELETE
    public void delete(UUID id) {
        repository.deleteById(id);
    }

    private int calculateSize(TestCaseRequest r) {
        return (r.getInput().length() + r.getExpectedOutput().length()) / 1024;
    }
}

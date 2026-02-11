package com.devpro.problem_service.service;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.devpro.problem_service.dto.TestCaseRequest;
import com.devpro.problem_service.model.CustomResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

import com.devpro.problem_service.dto.ProblemRequest;
import com.devpro.problem_service.model.Problem;
import com.devpro.problem_service.repository.ProblemRepository;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProblemService {

    private final ProblemRepository repository;
    private final TestCaseService testCaseService;
    private final CloudinaryService cloudinaryService;

    private void map(ProblemRequest request, Problem p, List<MultipartFile> composeFiles) {

        Map<String, String> mp = new HashMap<>();

        p.setTitle(request.getTitle());
        p.setDescription(request.getDescription());
        p.setDifficulty(request.getDifficulty());
        p.setTags(request.getTags());
        p.setIsActive(true);
        p.setServices(request.getServices());
        p.setCpuLimit(request.getCpuLimit());
        p.setKeys(request.getKeys());
//        p.setImageName(request.getImageName());
        p.setEntryFile(request.getEntryFile());
        p.setMemoryLimitMB(request.getMemoryLimitMB());
        p.setTimeLimitSeconds(request.getTimeLimitSeconds());

        if (composeFiles != null && !composeFiles.isEmpty()) {

            for (MultipartFile file : composeFiles) {

                if (file == null || file.isEmpty()) continue;

                String filename = file.getOriginalFilename();
                if (filename == null) continue;

                filename = filename.toLowerCase();

                String uploadedId = cloudinaryService.addFile(file);

                if (filename.contains("js")) {
                    mp.put("js-express", uploadedId);
                }
                else if (filename.contains("ts")) {
                    mp.put("ts-express", uploadedId);
                }
                else if (filename.contains("py") || filename.contains("fastapi")) {
                    mp.put("py-fastapi", uploadedId);
                }
            }
        }

        p.setComposeFile(mp);

    }

    public ProblemService(ProblemRepository repository, TestCaseService testCaseService, CloudinaryService cloudinaryService) {
        this.repository = repository;
        this.testCaseService = testCaseService;
        this.cloudinaryService = cloudinaryService;
    }

    // CREATE
    public CustomResponse create(ProblemRequest request, List<MultipartFile> composeFiles) throws JsonProcessingException {
        Problem p = new Problem();
        map(request, p,  composeFiles);
        p = repository.save(p);

        for (TestCaseRequest testCaseRequest : request.getTestCases()) {
            TestCaseRequest request1 = new TestCaseRequest();
            request1.setProblemId(p.getId());
            request1.setInput(testCaseRequest.getInput());
            request1.setExpectedOutput(testCaseRequest.getExpectedOutput());
            request1.setIsHidden(testCaseRequest.getIsHidden());
            request1.setExpectedStatus(testCaseRequest.getExpectedStatus());
            request1.setEndpoint(testCaseRequest.getEndpoint());
            request1.setMethod(testCaseRequest.getMethod());
            testCaseService.create(request1);
        }
        Map<String, Object> data = Map.of("problem", p);

        return new CustomResponse(
                data,
                "Successfully created problem",
                201,
                ""
        );
    }

    // READ ALL
    public CustomResponse getAll() {
        List<Problem> problems = repository.findAll();
        System.out.println(problems.size() + " " + problems.getFirst().toString());
        Map<String, Object> DATA = Map.of("problems", problems);
        return new CustomResponse(
                DATA,
                "Problems fetched successfully.",
                200,
                ""
        );
    }

    // READ BY ID
    public CustomResponse getById(UUID id) {
        Problem p = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Problem not found"));

        return new CustomResponse(
                Map.of("problem", p),
                "Problem fetched successfully.",
                200,
                ""
        );
    }

    public Problem getByIdRaw(UUID id) {

        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Problem not found"));
    }

    private Problem findProblemById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Problem not found"));
    }

//    public CustomResponse update(UUID id, ProblemRequest request) {
//        Problem p = findProblemById(id);
//        map(request, p);
//        Problem updated = repository.save(p);
//
//        return new CustomResponse(
//                Map.of("problem", updated),
//                "Problem updated successfully.",
//                200,
//                ""
//        );
//    }

    //get file Url - downloadable
    public String getFileUrl(String publicId){
        return cloudinaryService.getFile(publicId);
    }

    //  Delete
    public CustomResponse delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("Problem not found");
        }

        repository.deleteById(id);

        return new CustomResponse(
                Map.of("problemId", id),
                "Problem deleted permanently",
                200,
                ""
        );
    }

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

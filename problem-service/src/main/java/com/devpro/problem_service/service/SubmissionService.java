package com.devpro.problem_service.service;

import com.devpro.problem_service.clients.UserHelper;
import com.devpro.problem_service.dto.SubmissionRequest;
import com.devpro.problem_service.dto.ProfileUpdateRequest;
import com.devpro.problem_service.model.CustomResponse;
import com.devpro.problem_service.model.Problem;
import com.devpro.problem_service.model.Submission;
import com.devpro.problem_service.model.SubmissionStatus;
import com.devpro.problem_service.repository.ProblemRepository;
import com.devpro.problem_service.repository.SubmissionRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final UserHelper helper;
    private final ProblemRepository problemRepository;
    private static final String HEADER_NAME = "X-User-Id";

    public SubmissionService(SubmissionRepository submissionRepository, UserHelper helper, ProblemRepository problemRepository) {
        this.submissionRepository = submissionRepository;
        this.helper = helper;
        this.problemRepository = problemRepository;
    }

    public CustomResponse saveSubmission(SubmissionRequest api){
        try {
            log.info("Saving submission - {}", api.toString());

            //get a problem
            Problem problem = problemRepository.findById(api.getProblemId()).orElse(null);

            if(problem == null){
                log.info("Problem is null");
                return new CustomResponse(null, "Problem not found", 404, "");
            }

            //create submission
            Submission submission = new Submission();
            submission.setProblemId(api.getProblemId());
            submission.setUserId(api.getUserId());
            submission.setFramework(api.getFramework());
            submission.setStatus(api.getStatus());
            submission.setExecutionTimeMs(api.getExecutionTimeMs());
            submission.setMemoryUsedMB(api.getMemoryUsedMB());
            submission.setTotalTestcases(api.getTotalTestcases());
            submission.setTestcasesPassed(api.getTestcasesPassed());
            log.info("Submission created");

            //check a problem is already done or not
            boolean isAlreadyDone = submissionRepository.existsByUserIdAndProblemIdAndStatus(api.getUserId(), api.getProblemId(), SubmissionStatus.ACCEPTED);

            //submission add
            submissionRepository.save(submission);
            //call user's api update that
            ProfileUpdateRequest request = new ProfileUpdateRequest(submission, problem, isAlreadyDone);
            log.info("profile updated api called - and isAlreadyDone - {} ", isAlreadyDone);
            CustomResponse response = helper.profileUpdate(request);

            if (response.getError() != null){
                return new CustomResponse(null, response.getError(), 500, "");
            }

            return new CustomResponse(Map.of("submission", submission), "Submission saved successfully", 201, "");

        }catch (Exception e){
            return new CustomResponse(null, e.getMessage(), 500, "");
        }
    }

    public CustomResponse getSubmission(HttpServletRequest request, String problemId){
        try{
            String userId = request.getHeader(HEADER_NAME);
            log.info("Getting submission by id {} for user {}", problemId, userId);
            List<Submission> submissions = submissionRepository.findAllByUserIdAndProblemId(userId, UUID.fromString(problemId));
            return new CustomResponse(Map.of("submission", submissions), "Submissions found", 200, "");
        }catch (Exception e){
            return new CustomResponse(null, e.getMessage(), 500, "");
        }
    }
}

package com.devpro.code_runner_service.helper;

import com.devpro.code_runner_service.DTO.CustomResponse;
import com.devpro.code_runner_service.DTO.PreviewURL;
import com.devpro.code_runner_service.DTO.SubmissionRequest;
import com.devpro.code_runner_service.DTO.SubmissionStatus;
import com.devpro.code_runner_service.clients.ProblemClient;
import com.devpro.code_runner_service.config.socket_configs.LogWebSocketHandler;
import com.devpro.code_runner_service.models.Problem;
import com.devpro.code_runner_service.models.TestCase;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Slf4j
@Service
public class TestCaseHelper {

    private final ProblemClient problemClient;
    private final WebClient webClient;
    private final LogWebSocketHandler logWebSocketHandler;

    public TestCaseHelper(ProblemClient problemClient, WebClient webClient, LogWebSocketHandler logWebSocketHandler) {
        this.problemClient = problemClient;
        this.webClient = webClient;
        this.logWebSocketHandler = logWebSocketHandler;
    }

    private List<TestCase> getTestCase(String uuid) {
        return problemClient.getTestCases(uuid);
    }

    public String getPublicUrl(String publicId) {
        return problemClient.getPublicUrl(publicId);
    }

    public Problem getProblemById(String uuid) {
        return problemClient.getProblem(uuid);
    }

    public CustomResponse createSubmission(SubmissionRequest submissionRequest) {
        log.info("Request is send...........");
        return problemClient.saveSubmission(submissionRequest);
    }

    private List<TestCase> getSampleTestCases(String uuid) {
        return problemClient.getTestCases(uuid)
                .stream()
                .filter(tc -> !tc.getIsHidden())
                .toList();
    }

    // ---------------------------------------------------
    // 🔥 MAIN TESTCASE EXECUTION ENGINE
    // ---------------------------------------------------
    private CustomResponse testCaseChecker(
            List<TestCase> testCases,
            PreviewURL url,
            Boolean isSample,
            String executionId
    ) {

        log.info("ExecutionId={} | Starting Testcase Execution | Total={}",
                executionId,
                testCases.size());

        int passedCount = 0;
        List<Map<String, Object>> testCaseReports = new ArrayList<>();
        Map<String, Object> DATA = new HashMap<>();

        for (int i = 0; i < testCases.size(); i++) {

            TestCase testCase = testCases.get(i);
            Map<String, Object> report = new HashMap<>();

            Instant startTime = Instant.now();

            log.info("ExecutionId={} | Running TestCase #{} | {} {} | {} | {}",
                    executionId,
                    i + 1,
                    testCase.getMethod(),
                    testCase.getEndpoint(),
                    testCase.getExpectedStatus(),
                    testCase.getExpectedOutputJson());

            report.put("testCaseNo", i + 1);
            report.put("method", testCase.getMethod());
            report.put("endpoint", testCase.getEndpoint());
            report.put("input", testCase.getInputJson());
            report.put("expectedStatus", testCase.getExpectedStatus());
            report.put("expectedBody", testCase.getExpectedOutputJson());

            boolean isPassed = false;

            try {

                HttpMethod httpMethod =
                        HttpMethod.valueOf(testCase.getMethod().toString());

                // Build request
                WebClient.RequestBodySpec request = webClient
                        .method(httpMethod)
                        .uri(url.getUrl() + testCase.getEndpoint())
                        .accept(MediaType.APPLICATION_JSON);

                // Only attach body for non-GET methods
                if (!httpMethod.equals(HttpMethod.GET) && testCase.getInputJson() != null) {
                    request.bodyValue(testCase.getInputJson());
                }

                // IMPORTANT: use exchangeToMono so 4xx/5xx don't throw exception
                ResponseEntity<JsonNode> responseEntity =
                        request.exchangeToMono(response ->
                                response.toEntity(JsonNode.class)
                        ).block();

                int actualStatus = responseEntity.getStatusCodeValue();
                JsonNode actualBody = responseEntity.getBody();

                report.put("actualStatus", actualStatus);
                report.put("actualBody", actualBody);

                log.info("ExecutionId={} | TestCase #{} | ExpectedStatus={} | ActualStatus={} | expectedBody={} | actualBody={}",
                        executionId,
                        i + 1,
                        testCase.getExpectedStatus(),
                        actualStatus,
                        testCase.getExpectedOutputJson(),
                        actualBody);

                // Compare JSON safely
                boolean bodyMatch =
                        Objects.equals(actualBody, testCase.getExpectedOutputJson());

                if (actualStatus == testCase.getExpectedStatus() && bodyMatch) {

                    report.put("status", "PASSED");
                    isPassed = true;
                    passedCount++;

                    log.info("ExecutionId={} | TestCase #{} PASSED ✅",
                            executionId,
                            i + 1);

                } else {

                    report.put("status", "FAILED");
                    report.put("error",
                            actualStatus != testCase.getExpectedStatus()
                                    ? "Status code mismatch"
                                    : "Response body mismatch");

                    log.warn("ExecutionId={} | TestCase #{} FAILED ❌ | Reason={}",
                            executionId,
                            i + 1,
                            report.get("error"));
                }

            } catch (Exception ex) {

                // Real errors only (timeout, connection failure, crash)
                report.put("status", "FAILED");
                report.put("error", ex.getMessage());

                log.error("ExecutionId={} | TestCase #{} ERROR ❌ | {}",
                        executionId,
                        i + 1,
                        ex.getMessage());
            }

            // ⏱ Execution time
            long timeTaken =
                    Duration.between(startTime, Instant.now()).toMillis();

            report.put("executionTimeMs", timeTaken);

            log.info("ExecutionId={} | TestCase #{} | Time={} ms",
                    executionId,
                    i + 1,
                    timeTaken);

            // Early exit for SUBMIT mode
            if (!isSample && (!"PASSED".equals(report.get("status")) || testCases.size() == passedCount )   ) {
                log.info("In broooooooooooooooooooooooooooooooooooooooooooo wow in submittttttttttttttttttttttttttttttttttttttttttttt");

                DATA.put("TotalTestcases", testCases.size());
                DATA.put("PassedTestcases", passedCount);
                DATA.put("FailedAt", i + 1);
                DATA.put("Result", report);

                DATA.put("Status",
                        passedCount == testCases.size()
                                ? SubmissionStatus.ACCEPTED
                                : SubmissionStatus.WRONG_ANSWER);

                log.warn("ExecutionId={} | Submission stopped at TestCase #{}",
                        executionId,
                        i + 1);

                return new CustomResponse(
                        DATA,
                        "Wrong Answer",
                        200,
                        "Testcase " + (i + 1) + " failed"
                );
            }

            testCaseReports.add(report);
        }

        // Final summary
        DATA.put("TotalTestcases", testCases.size());
        DATA.put("PassedTestcases", passedCount);
        DATA.put("FailedTestcases", testCases.size() - passedCount);
        DATA.put("Reports", testCaseReports);

        log.info("ExecutionId={} | Finished | Passed={} | Failed={}",
                executionId,
                passedCount,
                testCases.size() - passedCount);

        logWebSocketHandler.sendEvent(
                executionId,
                "TESTCASE",
                new CustomResponse(
                        DATA,
                        "All testcases executed",
                        200,
                        "Execution finished"
                )
        );

        return new CustomResponse(
                DATA,
                "execution done",
                200,
                null
        );
    }

    // ---------------------------------------------------
    // PUBLIC METHODS
    // ---------------------------------------------------

    public void codeRun(String problemId, PreviewURL url, String executionId) {
        List<TestCase> testCases = getSampleTestCases(problemId);
        testCaseChecker(testCases, url, true, executionId);
    }

    public CustomResponse codeSubmit(String problemId, PreviewURL url, String executionId) {
        List<TestCase> testCases = getTestCase(problemId);
        return testCaseChecker(testCases, url, false, executionId);
    }
}

package com.devpro.code_runner_service.helper;

import com.devpro.code_runner_service.DTO.CustomResponse;
import com.devpro.code_runner_service.DTO.PreviewURL;
import com.devpro.code_runner_service.models.Problem;
import com.devpro.code_runner_service.models.TestCase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.*;

@Service
public class TestCaseHelper {
    private final TestCaseClient testCaseClient;
    private final WebClient webClient;

    public TestCaseHelper(TestCaseClient testCaseClient, WebClient webClient) {
        this.testCaseClient = testCaseClient;
        this.webClient = webClient;
    }

    private List<TestCase> getTestCase(String uuid) {
        return testCaseClient.getTestCases(uuid);
    }

    public String getPublicUrl(String publicId){
        return testCaseClient.getPublicUrl(publicId);
    }

    public Problem getProblemById(String uuid){
        return testCaseClient.getProblem(uuid);
    }

    private List<TestCase> getSampleTestCases(String uuid) {
        return testCaseClient.getTestCases(uuid).stream().filter((tc) -> tc.getIsHidden() == false).toList();
    }

    private CustomResponse TestCaseChecker(
            List<TestCase> testCases,
            PreviewURL url,
            Boolean isSample
    ) {

        int passedCount = 0;
        List<Map<String, Object>> testCaseReports = new ArrayList<>();
        Map<String, Object> DATA = new HashMap<>();

        for (int i = 0; i < testCases.size(); i++) {

            TestCase testCase = testCases.get(i);
            Map<String, Object> report = new HashMap<>();

            report.put("testCaseNo", i + 1);
            report.put("method", testCase.getMethod());
            report.put("endpoint", testCase.getEndpoint());
            report.put("input", testCase.getInputJson());
            report.put("expectedStatus", testCase.getExpectedStatus());
            report.put("expectedBody", testCase.getExpectedOutputJson());

            boolean isPassed = false;

            try {

                HttpMethod httpMethod = HttpMethod.valueOf(testCase.getMethod().toString());

                ResponseEntity<JsonNode> responseEntity = webClient
                        .method(httpMethod)
                        .uri(url.getUrl() + testCase.getEndpoint())
                        .accept(MediaType.APPLICATION_JSON)
                        .bodyValue(testCase.getInputJson())
                        .retrieve()
                        .toEntity(JsonNode.class)
                        .block();

                int actualStatus = responseEntity.getStatusCodeValue();
                JsonNode actualBody = responseEntity.getBody();

                report.put("actualStatus", actualStatus);
                report.put("actualBody", actualBody);

                if (actualStatus == testCase.getExpectedStatus()
                        && Objects.equals(actualBody, testCase.getExpectedOutputJson())) {

                    report.put("status", "PASSED");
                    isPassed = true;
                    passedCount++;

                } else {

                    report.put("status", "FAILED");
                    report.put("error",
                            actualStatus != testCase.getExpectedStatus()
                                    ? "Status code mismatch"
                                    : "Response body mismatch"
                    );
                }

            } catch (WebClientResponseException ex) {

                int actualStatus = ex.getStatusCode().value();
                report.put("actualStatus", actualStatus);

                try {
                    JsonNode actualBody = new ObjectMapper()
                            .readTree(ex.getResponseBodyAsString());
                    report.put("actualBody", actualBody);
                } catch (Exception e) {
                    report.put("actualBody", ex.getResponseBodyAsString());
                }

                if (actualStatus == testCase.getExpectedStatus()) {

                    try {
                        JsonNode actualBody = new ObjectMapper()
                                .readTree(ex.getResponseBodyAsString());

                        if (actualBody.equals(testCase.getExpectedOutputJson())) {
                            report.put("status", "PASSED");
                            isPassed = true;
                            passedCount++;
                        } else {
                            report.put("status", "FAILED");
                            report.put("error", "Response body mismatch");
                        }

                    } catch (Exception e) {
                        report.put("status", "FAILED");
                        report.put("error", "Invalid response body");
                    }

                } else {
                    report.put("status", "FAILED");
                    report.put("error", "Status code mismatch");
                }

            } catch (Exception ex) {

                report.put("status", "FAILED");
                report.put("error", ex.getMessage());
            }

            // -----------------------------
            // LEETCODE STYLE EXIT
            // -----------------------------
            if (!isSample && !"PASSED".equals(report.get("status"))) {

                DATA.put("PassedTestcases", passedCount);
                DATA.put("FailedAt", i + 1);
                DATA.put("Result", report);

                return new CustomResponse(
                        DATA,
                        "Wrong Answer",
                        200,
                        "Testcase " + (i + 1) + " failed"
                );
            }

            testCaseReports.add(report);
        }

        // -----------------------------
        // SAMPLE MODE FULL REPORT
        // -----------------------------
        DATA.put("TotalTestcases", testCases.size());
        DATA.put("PassedTestcases", passedCount);
        DATA.put("FailedTestcases", testCases.size() - passedCount);
        DATA.put("Reports", testCaseReports);

        return new CustomResponse(
                DATA,
                "All testcases executed",
                200,
                "Execution finished"
        );
    }



    public CustomResponse codeRun(String uuid, PreviewURL url) {

        //get test-cases
        List<TestCase> testCases = getSampleTestCases(uuid);

        //check one by one
        return TestCaseChecker(testCases, url, true);
    }

    public CustomResponse codeSubmit(String uuid, PreviewURL url) {
        //get-testcase
        List<TestCase> testCases = getTestCase(uuid);

        //check one by one
        return TestCaseChecker(testCases, url, false);
    }
}

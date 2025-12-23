package com.devpro.code_runner_service.helper;

import com.devpro.code_runner_service.DTO.CustomResponse;
import com.devpro.code_runner_service.DTO.PreviewURL;
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

    private List<TestCase> getSampleTestCases(String uuid) {
        return testCaseClient.getTestCases(uuid).stream().filter((tc) -> tc.getIsHidden() == false).toList();
    }

    private CustomResponse TestCaseChecker(List<TestCase> testCases, PreviewURL url) {

        int passedCount = 0;
        Map<String, Object> DATA = new HashMap<>();

        System.out.println("======================================");
        System.out.println(" Starting Test Case Execution ");
        System.out.println(" Total Testcases: " + testCases.size());
        System.out.println(" Base URL: " + url.getUrl());
        System.out.println("======================================");

        for (int i = 0; i < testCases.size(); i++) {

            TestCase testCase = testCases.get(i);

            System.out.println("\n--------------------------------------");
            System.out.println(" Running Testcase #" + (i + 1));
            System.out.println(" Method   : " + testCase.getMethod());
            System.out.println(" Endpoint : " + testCase.getEndpoint());
            System.out.println(" Input    : " + testCase.getInputJson());
            System.out.println(" Expected Status : " + testCase.getExpectedStatus());
            System.out.println("--------------------------------------");

            HttpMethod httpMethod = HttpMethod.valueOf(testCase.getMethod().toString());

            ResponseEntity<JsonNode> responseEntity;

            try {
                responseEntity = webClient
                        .method(httpMethod)
                        .uri(url.getUrl() + testCase.getEndpoint())
                        .accept(MediaType.APPLICATION_JSON)
                        .bodyValue(testCase.getInputJson())
                        .retrieve()
                        .toEntity(JsonNode.class)
                        .block();
            } catch (WebClientResponseException ex) {

                int actualStatus = ex.getStatusCode().value();

                if (actualStatus != testCase.getExpectedStatus()) {
                    System.out.println("❌ Status Mismatch");

                    DATA.put("TotalTestcases", testCases.size());
                    DATA.put("PassedTestcases", passedCount);

                    return new CustomResponse(
                            DATA,
                            "Testcase " + (i + 1) + " failed",
                            500,
                            "Status code mismatch"
                    );
                }

                try {
                    JsonNode actualBody = new ObjectMapper().readTree(ex.getResponseBodyAsString());
                    System.out.println("Actual Body: " + actualBody.toPrettyString());
                    System.out.println("Expected Body: " + testCase.getExpectedOutputJson().toPrettyString());
                    if (!actualBody.equals(testCase.getExpectedOutputJson())) {

                        System.out.println("❌ Response Body Mismatch");

                        DATA.put("TotalTestcases", testCases.size());
                        DATA.put("PassedTestcases", passedCount);

                        return new CustomResponse(
                                DATA,
                                "Testcase " + (i + 1) + " failed",
                                500,
                                "Response body mismatch"
                        );
                    }

                } catch (Exception parseEx) {
                    return new CustomResponse(
                            null,
                            "Invalid response body",
                            500,
                            parseEx.getMessage()
                    );
                }

                System.out.println("✅ Testcase Passed");
                passedCount++;
                continue;
            }

            int actualStatus = responseEntity.getStatusCodeValue();
            JsonNode actualBody = responseEntity.getBody();

            if (actualStatus != testCase.getExpectedStatus()) {

                System.out.println("❌ Status Mismatch");
                System.out.println(" Expected : " + testCase.getExpectedStatus());
                System.out.println(" Actual   : " + actualStatus);

                DATA.put("TotalTestcases", testCases.size());
                DATA.put("PassedTestcases", passedCount);

                return new CustomResponse(
                        DATA,
                        "Testcase " + (i + 1) + " failed",
                        500,
                        "Status code mismatch"
                );
            }

            if (!Objects.equals(actualBody, testCase.getExpectedOutputJson())) {

                System.out.println("❌ Response Body Mismatch");
                System.out.println(" Expected Body : "
                        + testCase.getExpectedOutputJson().toPrettyString());
                System.out.println(" Actual Body   : "
                        + (actualBody != null ? actualBody.toPrettyString() : "null"));

                DATA.put("TotalTestcases", testCases.size());
                DATA.put("PassedTestcases", passedCount);

                return new CustomResponse(
                        DATA,
                        "Testcase " + (i + 1) + " failed",
                        500,
                        "Response body mismatch"
                );
            }

            System.out.println("✅ Testcase Passed");
            passedCount++;
        }

        System.out.println("\n======================================");
        System.out.println(" ✅ ALL TEST CASES PASSED ");
        System.out.println(" Passed: " + passedCount + "/" + testCases.size());
        System.out.println("======================================");

        DATA.put("TotalTestcases", testCases.size());
        DATA.put("PassedTestcases", passedCount);
        DATA.put("message", "All test cases passed");

        return new CustomResponse(
                DATA,
                "All test cases passed",
                200,
                "All test cases passed"
        );
    }



    public CustomResponse codeRun(String uuid, PreviewURL url) {

        //get test-cases
        List<TestCase> testCases = getSampleTestCases(uuid);

        //check one by one
        return TestCaseChecker(testCases, url);
    }

    public CustomResponse codeSubmit(String uuid, PreviewURL url) {
        //get-testcase
        List<TestCase> testCases = getTestCase(uuid);

        //check one by one
        return TestCaseChecker(testCases, url);
    }
}

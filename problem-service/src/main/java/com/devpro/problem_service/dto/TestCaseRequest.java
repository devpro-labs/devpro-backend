package com.devpro.problem_service.dto;

import java.util.UUID;

import com.devpro.problem_service.model.Method;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestCaseRequest {

    private UUID problemId;

    // Raw input
    private JsonNode input;
    private JsonNode expectedOutput;

    private Integer expectedStatus;
    private Boolean isHidden;
//    private Integer weight;

    private Method method;
    private String endpoint;
}

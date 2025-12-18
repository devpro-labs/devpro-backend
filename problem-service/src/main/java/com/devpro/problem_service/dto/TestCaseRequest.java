package com.devpro.problem_service.dto;

import java.util.UUID;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestCaseRequest {

    private UUID problemId;

    // Raw input
    private String input;
    private String expectedOutput;

    private Integer expectedStatus;
    private Boolean isHidden;
    private Integer weight;
}

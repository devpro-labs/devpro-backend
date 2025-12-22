package com.devpro.problem_service.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProblemResponse {

    private UUID id;
    private String title;
    private String slug;
    private String difficulty;
    private String category;
    private Boolean isActive;
}

package com.devpro.problem_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProblemRequest {

    private String title;
    private String slug;
    private String description;
    private String difficulty;
    private String category;
}

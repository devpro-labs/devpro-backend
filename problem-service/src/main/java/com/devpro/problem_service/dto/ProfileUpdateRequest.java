package com.devpro.problem_service.dto;

import com.devpro.problem_service.model.Problem;
import com.devpro.problem_service.model.Submission;
import lombok.*;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfileUpdateRequest {
    Submission submission;
    Problem problem;
}

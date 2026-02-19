package com.devpro.problem_service.dto;


import com.devpro.problem_service.model.SubmissionStatus;
import lombok.*;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class SubmissionRequest {
    UUID problemId;
    String userId;
    String framework;
    Integer testcasesPassed;
    Integer totalTestcases;
    SubmissionStatus status;
    Long executionTimeMs;
    Integer memoryUsedMB;
}

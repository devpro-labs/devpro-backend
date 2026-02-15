package com.devpro.user_service.dto;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class Submission {
    private UUID id;
    private UUID problemId;
    private String userId;

    private String framework;

    private Integer testcasesPassed;
    private Integer totalTestcases;

    private SubmissionStatus status;

    private Long executionTimeMs;
    private Integer memoryUsedMB;

    private Instant submittedAt;
}

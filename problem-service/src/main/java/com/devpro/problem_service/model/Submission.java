package com.devpro.problem_service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "submissions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Submission {

    @Id
    @GeneratedValue
    private UUID id;

    // Which problem this submission belongs to
    @Column(name = "problem_id", nullable = false)
    private UUID problemId;

    // Who submitted it
    @Column(name = "user_id", nullable = false)
    private UUID userId;

    // How many test cases passed
    @Column(name = "testcases_passed")
    private Integer testcasesPassed;

    // Total test cases
    @Column(name = "total_testcases")
    private Integer totalTestcases;

    // Final verdict
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubmissionStatus status;

    // Execution time (ms)
    @Column(name = "execution_time_ms")
    private Long executionTimeMs;

    // Memory used (MB)
    @Column(name = "memory_used_mb")
    private Integer memoryUsedMB;

    // When submission was made
    @Column(name = "submitted_at")
    private Instant submittedAt = Instant.now();
}

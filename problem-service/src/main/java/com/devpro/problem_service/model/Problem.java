package com.devpro.problem_service.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "problems")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Problem {

    @Id
    @GeneratedValue
    private UUID id;

    @Column
    private String title;

    @Column
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column
    private String difficulty;
    @Column
    private String category;

    @Column(columnDefinition = "TEXT[]")
    private List<String> expectedFrameworks;

    @Column
    private Boolean isActive = true;

    @Column
    private Instant createdAt = Instant.now();

}

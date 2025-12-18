package com.devpro.problem_service.model;

import java.util.UUID;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "test_cases")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestCase {

    @Id
    @GeneratedValue
    private UUID id;

    private UUID problemId;

    @Column(columnDefinition = "jsonb")
    private String inputJson;

    @Column(columnDefinition = "jsonb")
    private String expectedOutputJson;

    private String inputFileUrl;
    private String expectedOutputFileUrl;

    @Enumerated(EnumType.STRING)
    private StorageType storageType;

    private Integer sizeKb;
    private Integer expectedStatus;
    private Boolean isHidden;
    private Integer weight;
}



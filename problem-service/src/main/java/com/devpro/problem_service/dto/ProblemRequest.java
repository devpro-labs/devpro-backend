package com.devpro.problem_service.dto;

import com.devpro.problem_service.model.ServiceType;
import com.fasterxml.jackson.databind.JsonNode;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProblemRequest {

    private String title;
    private String description;
    private String difficulty;
    private List<String> tags;

    private JsonNode imageName;
    private String entryFile;
    private List<ServiceType> services;

    private JsonNode  keys;

    private Integer timeLimitSeconds;
    private Integer memoryLimitMB;
    private Double cpuLimit;

    private List<TestCaseRequest>  testCases;

}

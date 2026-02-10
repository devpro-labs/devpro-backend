package com.devpro.code_runner_service.models;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Problem {

    private UUID id;

    private String title;

    private String description;

    private String difficulty;

    private List<String> tags;

    private JsonNode imageName;

    private String entryFile;

    private List<ServiceType> services;

    private Map<String, String> composeFile;


    private JsonNode keys;

    private Integer timeLimitSeconds;

    private Integer memoryLimitMB;

    private Double cpuLimit;

    private Boolean isActive;

    private Instant createdAt;
}

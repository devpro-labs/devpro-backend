package com.devpro.user_service.dto;


import com.fasterxml.jackson.databind.JsonNode;
import lombok.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class Problem {

    private UUID id;
    private String title;
    private Difficulty difficulty;

    private List<String> tags;
    private List<String> services;   // better to send as string names
    private String entryFile;

    private Map<String, String> composeFile;
    private JsonNode keys;

    private Integer timeLimitSeconds;
    private Integer memoryLimitMB;
    private Double cpuLimit;

    private Boolean isActive;
}

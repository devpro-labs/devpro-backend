package com.devpro.problem_service.dto;

import com.devpro.problem_service.model.Difficulty;
import com.devpro.problem_service.model.ServiceType;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
@JsonIgnoreProperties(ignoreUnknown = true)

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ProblemRequest {

    private String title;
    private String description;
    private Difficulty difficulty;
    private List<String> tags = new ArrayList<>();
    private String entryFile;
    private List<ServiceType> services = new ArrayList<>();

    private JsonNode keys;

    private Integer timeLimitSeconds;
    private Integer memoryLimitMB;
    private Double cpuLimit;

    private List<TestCaseRequest> testCases = new ArrayList<>();
}

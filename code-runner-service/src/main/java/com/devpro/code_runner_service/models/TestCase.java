package com.devpro.code_runner_service.models;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TestCase {
    private UUID id;

    private UUID problemId;

    private JsonNode inputJson;

    private JsonNode expectedOutputJson;

    private String inputFileUrl;
    private String expectedOutputFileUrl;

    private StorageType storageType;

    private Integer sizeKb;
    private Integer expectedStatus;
    private Boolean isHidden;

    private Method method;
    private String endpoint;
}

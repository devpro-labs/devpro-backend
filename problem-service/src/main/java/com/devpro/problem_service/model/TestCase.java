package com.devpro.problem_service.model;

import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

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

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private JsonNode inputJson;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private JsonNode expectedOutputJson;

    private String inputFileUrl;
    private String expectedOutputFileUrl;

    @Enumerated(EnumType.STRING)
    private StorageType storageType;

    private Integer sizeKb;
    private Integer expectedStatus;
    private Boolean isHidden;
}



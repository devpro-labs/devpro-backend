package com.devpro.problem_service.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

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

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column
    private String difficulty;

    @Column
    private List<String> tags;


    @Column(nullable = false)
    private String entryFile;

    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private List<ServiceType> services;

    /* docker compose template for this problem */
    @Type(JsonBinaryType.class)
    @Column(name = "compose_file", columnDefinition = "jsonb")
    private Map<String, String> composeFile;

    /* env */
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb", name = "problem_keys")
    private JsonNode keys;

    /* limits */
    @Column
    private Integer timeLimitSeconds;

    @Column
    private Integer memoryLimitMB;

    @Column
    private Double cpuLimit;

    @Column
    private Boolean isActive = true;

    @Column
    private Instant createdAt = Instant.now();
}

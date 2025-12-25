package com.devpro.problem_service.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;
import java.util.UUID;

@Service
public class FileStorageService {

    private final WebClient webClient;
    private final String supabaseUrl;
    private final String serviceKey;
    private final String bucket;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public FileStorageService(
            @Value("${supabase.url}") String supabaseUrl,
            @Value("${supabase.service-key}") String serviceKey,
            @Value("${supabase.bucket}") String bucket
    ) {
        this.supabaseUrl = supabaseUrl;
        this.serviceKey = serviceKey;
        this.bucket = bucket;

        this.webClient = WebClient.builder()
                .baseUrl(supabaseUrl)
                .defaultHeader("Authorization", "Bearer " + serviceKey)
                .defaultHeader("apikey", serviceKey)
                .build();
    }

    public String upload(String jsonContent, String prefix) {

        // Validate JSON
        try {
            objectMapper.readTree(jsonContent);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Invalid JSON", e);
        }

        String fileName = prefix + "-" + UUID.randomUUID() + ".json";

        webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/storage/v1/object/{bucket}/{file}")
                        .build(bucket, fileName)
                )
                .header("Content-Type", "application/json")
                .header("x-upsert", "true")
                .bodyValue(jsonContent)
                .retrieve()
                .toBodilessEntity()
                .block();

        // PRIVATE bucket → return internal path, not public URL
        return bucket + "/" + fileName;
    }

    // ================= DELETE =================
    public void delete(String filePath) {
        if (filePath == null) return;

        webClient.delete()
                .uri(uriBuilder -> uriBuilder
                        .path("/storage/v1/object/{path}")
                        .build(filePath))
                .retrieve()
                .toBodilessEntity()
                .block();
    }

    // ================= SIGNED URL (EXECUTION SERVICE) =================
    public String generateSignedUrl(String filePath, int expiresInSeconds) {

        return webClient.post()
                .uri("/storage/v1/object/sign/" + filePath)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("""
                    { "expiresIn": %d }
                """.formatted(expiresInSeconds))
                .retrieve()
                .bodyToMono(String.class)
                .map(res -> {
                    try {
                        return supabaseUrl + "/storage/v1" +
                                objectMapper.readTree(res).get("signedURL").asText();
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                })
                .block();
    }

    private void validateJson(String json) {
        try {
            objectMapper.readTree(json);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid JSON file content", e);
        }
    }
}

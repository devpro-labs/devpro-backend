package com.devpro.problem_service.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
public class FileStorageService {

    private final WebClient webClient;
    private final String supabaseUrl;
    private final String serviceKey;
    private final String bucket;

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

    /**
     * Uploads test case content as a JSON file to Supabase Storage
     */
    public String upload(String content, String prefix) {

        String fileName = prefix + "-" + UUID.randomUUID() + ".json";
        String path = bucket + "/" + fileName;

        webClient.put()
                .uri("/storage/v1/object/" + path)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(content.getBytes(StandardCharsets.UTF_8))
                .retrieve()
                .bodyToMono(String.class)
                .block();

        return buildPublicUrl(fileName);
    }

    /**
     * Generates public URL (works only if bucket is public)
     */
    private String buildPublicUrl(String fileName) {
        return supabaseUrl + "/storage/v1/object/public/"
                + bucket + "/" + fileName;
    }
}

package com.devpro.problem_service.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.charset.StandardCharsets;
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

        System.out.println(webClient);
    }

    /**
     * Uploads test case content as a JSON file to Supabase Storage
     */
    public String upload(String content, String prefix) {

        String fileName = prefix + "-" + UUID.randomUUID() + ".json";

        webClient.put()
                .uri(uriBuilder -> uriBuilder
                        .path("/storage/v1/object/{bucket}/{file}")
                        .build(bucket, fileName)
                )
                .header("Content-Type",  MediaType.APPLICATION_JSON_VALUE)
                .header("x-upsert", "true")
                .bodyValue(content)
                .retrieve()
                .toBodilessEntity()
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

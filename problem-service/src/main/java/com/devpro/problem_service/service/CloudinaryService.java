package com.devpro.problem_service.service;


import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.devpro.problem_service.model.Problem;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileWriter;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;
    @Value("${cloudinary.folder}")
    private String folderName;

    public CloudinaryService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    /**
     * string to file
     */

    public File stringToFile(String content, String filename) {
        try {
            File file = File.createTempFile(filename, ".yml");
            FileWriter writer = new FileWriter(file);
            writer.write(content);
            writer.close();
            return file;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create file", e);
        }
    }


    /**
     * Upload compose file (yaml/json/etc)
     */
    public String addFile(MultipartFile file) {
        try {
            Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "resource_type", "raw",
                    "folder", folderName
            ));

            return result.get("public_id").toString(); // store this in DB

        } catch (Exception e) {
            throw new RuntimeException("Cloudinary upload failed", e);
        }
    }

    /**
     * Get downloadable URL for compose file
     */
    public String getFile(String publicId) {
        if (!publicId.contains("/")) {
            publicId = "devpro-dev/" + publicId;
        }
        return cloudinary.url()
                .resourceType("raw")
                .generate(publicId);
    }

    public void deleteAllFilesByProblem(Problem problem) {
        try {

            if (problem.getComposeFile() == null || problem.getComposeFile().isEmpty()) {
                return;
            }

            List<String> publicIds = problem.getComposeFile()
                    .values()
                    .stream()
                    .filter(Objects::nonNull)
                    .toList();

            if (!publicIds.isEmpty()) {
                cloudinary.api().deleteResources(
                        publicIds,
                        ObjectUtils.asMap("resource_type", "raw")
                );
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to delete compose files from Cloudinary", e);
        }
    }

}
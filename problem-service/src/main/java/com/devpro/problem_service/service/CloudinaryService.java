package com.devpro.problem_service.service;


import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileWriter;
import java.util.Map;

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
}
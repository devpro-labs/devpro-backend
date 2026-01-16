package com.devpro.code_runner_service.DTO;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Data
public class DockerRunner {
    private String image_name;
    private String libOrFramework;
    private String file_name;
    private List<FileNode> files;

    // 🔹 Flag to indicate DB connection detected in any file
    private boolean dbConnectionDetected = false;
}

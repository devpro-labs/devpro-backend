package com.devpro.code_runner_service.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Data
@Getter
@Setter
public class FileNode {
    private String name;
    @JsonProperty("isFolder")
    private boolean isFolder;
    private String content;
    private List<FileNode> children;

    // 🔹 Flag to indicate DB connection detected in this file
    private boolean dbConnectionDetected = false;

    @JsonProperty("isFolder")
    public boolean isFolder() {
        return isFolder;
    }

    @JsonProperty("isFolder")
    public void setFolder(boolean isFolder) {
        this.isFolder = isFolder;
    }
}

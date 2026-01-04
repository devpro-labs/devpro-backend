package com.devpro.code_runner_service.DTO;

import lombok.Data;

import java.util.List;

@Data
public class FileNode {
    private String name;
    private boolean isFolder;
    private String content;
    private List<FileNode> children;
}

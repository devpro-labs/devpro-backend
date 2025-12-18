package com.devpro.code_runner_service.DTO;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class PreviewURL {
    private String url;
    private String containerId;
    private int port;
}

package com.devpro.code_runner_service.DTO;
import lombok.*;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class DockerRunner {
    private String image_name;
    private String code;
    private String file_name;
    private String libOrFramework;
}

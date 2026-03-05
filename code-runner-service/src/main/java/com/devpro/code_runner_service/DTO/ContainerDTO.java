package com.devpro.code_runner_service.DTO;

import lombok.*;

@Data
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ContainerDTO {
    String projectId;
    String fileId;
    String fileName;
}

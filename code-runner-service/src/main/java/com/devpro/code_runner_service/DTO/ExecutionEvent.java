package com.devpro.code_runner_service.DTO;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class ExecutionEvent {
    private String type;
    private Object data;
}

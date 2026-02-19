package com.devpro.code_runner_service.DTO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
@AllArgsConstructor
public class SocketEvent {
    String executionId;
    String type;
    Object data;
}

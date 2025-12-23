package com.devpro.code_runner_service.DTO;

import lombok.*;

import java.util.List;
import java.util.Map;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class CustomResponse {
    Map<String, Object> DATA;
    String MESSAGE;
    int STATUS;
    String ERROR;
}

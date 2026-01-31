package com.devpro.code_runner_service.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;
import java.util.Map;



@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class CustomResponse {
    @JsonProperty("DATA")
    Map<String, Object> data;

    @JsonProperty("MESSAGE")
    String message;

    @JsonProperty("STATUS")
    int status;

    @JsonProperty("ERROR")
    String error;
}

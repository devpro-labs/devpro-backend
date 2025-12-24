package com.devpro.problem_service.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

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

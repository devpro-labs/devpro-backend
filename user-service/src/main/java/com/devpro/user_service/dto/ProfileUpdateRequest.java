package com.devpro.user_service.dto;
import lombok.*;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfileUpdateRequest {
    Submission submission;
    Problem problem;
}

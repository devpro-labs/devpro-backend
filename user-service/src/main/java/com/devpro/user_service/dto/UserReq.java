package com.devpro.user_service.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class UserReq {
    private String username;
    private String email;
}

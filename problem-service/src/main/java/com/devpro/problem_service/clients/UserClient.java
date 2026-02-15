package com.devpro.problem_service.clients;

import com.devpro.problem_service.dto.ProfileUpdateRequest;
import com.devpro.problem_service.model.CustomResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "user-service")
public interface UserClient {

    @PostMapping("/api/user/profile")
    CustomResponse updateProfile(@RequestBody ProfileUpdateRequest request);
}

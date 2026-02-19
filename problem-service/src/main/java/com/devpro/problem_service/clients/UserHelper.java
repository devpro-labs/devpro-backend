package com.devpro.problem_service.clients;

import com.devpro.problem_service.dto.ProfileUpdateRequest;
import com.devpro.problem_service.model.CustomResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class UserHelper {

    private final UserClient userClient;

    public UserHelper(UserClient userClient) {
        this.userClient = userClient;
    }

    public CustomResponse profileUpdate(ProfileUpdateRequest request){
        log.info("Updating user profile {}", request.toString());
        return userClient.updateProfile(request);
    }
}

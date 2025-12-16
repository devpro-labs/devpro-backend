package com.devpro.user_service.controllers;

import com.devpro.user_service.model.User;
import com.devpro.user_service.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public User me(@AuthenticationPrincipal Jwt jwt) {
        String clerkId = jwt.getSubject(); // user_xxx
        return userRepository.getById(Long.valueOf(clerkId));
    }

    @GetMapping("/profile")
    public User profile(@AuthenticationPrincipal Jwt jwt) {
        String clerkId = jwt.getSubject();

        User user = userRepository.findById(Long.valueOf(clerkId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        return user;
    }
}

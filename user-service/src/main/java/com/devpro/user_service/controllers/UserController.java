package com.devpro.user_service.controllers;

import com.devpro.user_service.dto.ProfileUpdateRequest;
import com.devpro.user_service.dto.UserReq;
import com.devpro.user_service.model.Response;
import com.devpro.user_service.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public Response login(@RequestBody UserReq user) {
        return userService.checkUser(user);
    }

    @PostMapping("/profile")
    public Response profile(@RequestBody ProfileUpdateRequest profileUpdateRequest){
        return userService.updateProfile(profileUpdateRequest);
    }

    @GetMapping("/profile/{username}")
    public Response profile(@PathVariable String username){
        return userService.getProfile(username);
    }

//    @GetMapping("/profile")
//    public Response profile(){
//
//    }
}

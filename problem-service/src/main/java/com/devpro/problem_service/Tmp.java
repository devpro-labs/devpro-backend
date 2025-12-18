package com.devpro.problem_service;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/problems")
public class Tmp {

    @RequestMapping("/string")
    public ResponseEntity<Map<String, Object>> getProblem(
            @RequestHeader("X-User-Id") String userId
    ) {
        System.out.println(userId);
        return ResponseEntity.ok(Map.of("status", "ok"));
    }
}

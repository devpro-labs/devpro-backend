package com.devpro.code_runner_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication

public class CodeRunnerServiceApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(CodeRunnerServiceApplication.class);
        app.run(args);
    }

}
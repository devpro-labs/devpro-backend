package com.devpro.problem_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class ProblemServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ProblemServiceApplication.class, args);
	}

}

package com.devpro.problem_service.dto;

import java.util.UUID;

import com.devpro.problem_service.model.Problem;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProblemResponse {

   private Problem problem;
   private Boolean isSolved;
}

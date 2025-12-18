package com.devpro.problem_service.repository;

import com.devpro.problem_service.model.TestCase;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TestCaseRepository extends JpaRepository<TestCase, UUID> {

    List<TestCase> findByProblemId(UUID problemId);
}

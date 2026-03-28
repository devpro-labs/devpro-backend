package com.devpro.problem_service.repository;


import com.devpro.problem_service.model.Submission;
import com.devpro.problem_service.model.SubmissionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, UUID> {
    List<Submission> findAllByUserIdAndProblemId(String userid, UUID problemId);
    Boolean existsByUserIdAndProblemIdAndStatus(String userid, UUID problemId, SubmissionStatus status);

    @Query("SELECT DISTINCT s.problemId FROM Submission s where s.status= :status AND s.userId= :userId")
    List<UUID> findAllProblemIdByUserIdAndStatus(String userId, SubmissionStatus status);
}

package com.devpro.user_service.repository;

import com.devpro.user_service.model.Profile;
import com.devpro.user_service.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProfileRepository  extends JpaRepository<Profile, UUID> {
    Optional<Profile> findByUserId(String userId);
    void deleteByUserId(String userId);
}

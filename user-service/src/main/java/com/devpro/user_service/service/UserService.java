package com.devpro.user_service.service;

import com.devpro.user_service.dto.*;
import com.devpro.user_service.model.Profile;
import com.devpro.user_service.model.Response;
import com.devpro.user_service.model.User;
import com.devpro.user_service.repository.ProfileRepository;
import com.devpro.user_service.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UserService {


    private final UserRepository userRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ClerkWebhookVerify clerkWebhookVerify;
    private final ProfileRepository profileRepository;

    public UserService(UserRepository userRepository, ClerkWebhookVerify clerkWebhookVerify, ProfileRepository profileRepository) {
        this.userRepository = userRepository;
        this.clerkWebhookVerify = clerkWebhookVerify;
        this.profileRepository = profileRepository;
    }

    private Response helperCreateUser(JsonNode data) {
        log.info("session created event called");

        String userId = data.get("id").asText();
        String primaryEmailId = data.get("primary_email_address_id").asText();
        String username = data.get("username").asText();

        String email = null;
        for (JsonNode e : data.get("email_addresses")) {
            if (e.get("id").asText().equals(primaryEmailId)) {
                email = e.get("email_address").asText();
                break;
            }
        }

        if (email == null) {
            log.info("email address not found");
            return new Response(
                    null,
                    "Email is doesn't exist",
                    401,
                    "UnAuthorized"
            );
        }

        User exitsuser = userRepository.findByEmail(email).orElse(null);

        if(exitsuser != null){
            userRepository.delete(exitsuser);
            profileRepository.deleteByUserId(userId);
        }

        User user = new User();
        user.setId(userId);
        user.setEmail(email);
        user.setUsername(username);

        userRepository.save(user);
        log.info("user created");
        return new Response(
                Map.of("user", user),
                "User created",
                201,
                null
        );
    }

    private void helperCreateProfile(String userId){
        try{
            Profile p = new Profile();
            p.setUserId(userId);
            p.setTagStats(new HashMap<>());
            p.setFrameworkStats(new HashMap<>());
            p.setYearlyActivity(new HashMap<>());
            p.setTotalSolved(0L);
            p.setProSolved(0L);
            p.setCasualSolved(0L);
            p.setPro_maxSolved(0L);
            p.setEngineeringSolved(0L);
            p.setTotalSubmissions(0L);
            p.setCurrentStreak(0);
            p.setMaxStreak(0);
            p.setProfileViews(0L);
            p.setLastSubmissionAt(null);

            profileRepository.save(p);
        }catch (Exception e){
            log.error("Error while creating profile", e);
        }
    }


    public Response CreateUser(HttpServletRequest request) {

        try {
            log.info("webhooked called");
            String payload = request.getReader()
                    .lines()
                    .collect(Collectors.joining());

            clerkWebhookVerify.verify(request, payload);
            log.info("webhooked verified");


            JsonNode root = objectMapper.readTree(payload);
            String eventType = root.get("type").asText();
            JsonNode data = root.get("data");

            if ("user.created".equals(eventType)) {
                Response res =  helperCreateUser(data);
                //create profile
                helperCreateProfile(data.get("id").asText());
                return res;
            }
        } catch (Exception e) {
            log.warn("user checking failed - {}", e.getMessage());
            return new Response(
                    null,
                    e.getMessage(),
                    500,
                    e.getMessage()
            );
        }
        return null;
    }

    public Response checkUser(UserReq req) {
        try {
            log.info("user checking in db... for username-{} and email-{}", req.getUsername(), req.getEmail());
            User user = userRepository.findByUsernameAndEmail(req.getUsername(), req.getEmail()).orElse(null);

            if (user == null) {
                log.info("user not found");
                return new Response(
                        null,
                        "User is not exits",
                        401,
                        "UnAuthorized"
                );
            }
            log.info("user found");

            return new Response(
                    null,
                    "User logged in",
                    200,
                    null
            );
        } catch (Exception e) {
            log.warn("user checking failed - {}", e.getMessage());
            return new Response(
                    null,
                    e.getMessage(),
                    500,
                    e.getMessage()
            );
        }
    }

    @Transactional
    public Response updateProfile(ProfileUpdateRequest request) {
        try {
            log.info("Profile Update Request Received {}", request.toString());

            Problem problem = request.getProblem();
            Submission submission = request.getSubmission();

            String userId = submission.getUserId();

            // Fetch or create profile
            Profile profile = profileRepository.findByUserId(userId).orElse(null);
            if(profile == null){
                return new Response(null, "Profile not found", 404, "Profile not found");
            }

            // Always increment submission count
            profile.setTotalSubmissions(profile.getTotalSubmissions() + 1);

            // HEATMAP (YEARLY ACTIVITY)
            LocalDate today = LocalDate.now();
            String fullDate = today.toString(); // 2026-02-14 format

            profile.getYearlyActivity().merge(fullDate, 1, Integer::sum);

            // If Accepted → Update Stats
            if (!request.isAlreadyDone() && submission.getStatus() == SubmissionStatus.ACCEPTED) {

                // BASIC SOLVED STATS
                profile.setTotalSolved(profile.getTotalSolved() + 1);

                switch (problem.getDifficulty()) {
                    case PRO -> profile.setProSolved(profile.getProSolved() + 1);
                    case CASUAL -> profile.setCasualSolved(profile.getCasualSolved() + 1);
                    case PRO_MAX -> profile.setPro_maxSolved(profile.getPro_maxSolved() + 1);
                    case ENGINEER -> profile.setEngineeringSolved(profile.getEngineeringSolved() + 1);
                }

                // TAG-WISE STATS
                if (problem.getTags() != null) {
                    for (String tag : problem.getTags()) {
                        profile.getTagStats().merge(tag, 1L, Long::sum);
                    }
                }

                // FRAMEWORK STATS
                String framework = submission.getFramework();
                if (framework != null) {
                    profile.getFrameworkStats().merge(framework, 1L, Long::sum);
                }

                // Update Most Used Framework
                profile.setMostUsedFramework(
                        profile.getFrameworkStats()
                                .entrySet()
                                .stream()
                                .max(Map.Entry.comparingByValue())
                                .map(Map.Entry::getKey)
                                .orElse(null)
                );


                // update max streak
                profile.setMaxStreak(
                        Math.max(profile.getMaxStreak(), profile.getCurrentStreak())
                );

                // STREAK LOGIC
                Instant lastSubmission = profile.getLastSubmissionAt();

                if (lastSubmission != null) {
                    LocalDate lastDate = LocalDate.ofInstant(lastSubmission, ZoneId.systemDefault());

                    if (lastDate.equals(today.minusDays(1))) {
                        // consecutive day
                        profile.setCurrentStreak(profile.getCurrentStreak() + 1);
                    } else if (!lastDate.equals(today)) {
                        // reset streak
                        profile.setCurrentStreak(1);
                    }
                } else {
                    // first submission
                    profile.setCurrentStreak(1);
                }
            }

            // update last submission time
            profile.setLastSubmissionAt(Instant.now());

            log.info("Profile Updated Successfully {}", profile.toString());

            profile.setUpdatedAt(Instant.now());

            profileRepository.save(profile);

            return new Response(null, "Profile Updated", 200, null);

        } catch (Exception e) {
            log.error("Error while updating profile", e);
            return new Response(null, e.getMessage(), 500, e.getMessage());
        }
    }

    public Response getProfile(String userName) {
        try {
            User user = userRepository.findByUsername(userName).orElse(null);
            if (user == null) {
                return new Response(null, "User not found", 404, "User not found");
            }
            Profile profile = profileRepository.findByUserId(user.getId()).orElse(null);

            if (profile == null) {
                return new Response(null, "Profile not found", 404, "Profile not found");
            }

            return new Response(Map.of("profile", profile, "user", user), "Profile fetched", 200, null);

        } catch (Exception e) {
            return new Response(null, e.getMessage(), 500, e.getMessage());
        }
    }


//    public Response userProfile(HttpServletRequest request){
//        try{
//
//
//
//        }catch (Exception e){
//            return new Response(
//                    null,
//                    e.getMessage(),
//                    500,
//                    e.toString()
//            );
//        }
//    }

}

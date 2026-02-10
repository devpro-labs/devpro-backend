package com.devpro.user_service.service;

import com.devpro.user_service.dto.UserReq;
import com.devpro.user_service.model.Response;
import com.devpro.user_service.model.User;
import com.devpro.user_service.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.stream.Collectors;

@Service
public class UserService {


    private final UserRepository userRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ClerkWebhookVerify clerkWebhookVerify;
    private final Logger log = LoggerFactory.getLogger(UserService.class);

    public UserService(UserRepository userRepository,  ClerkWebhookVerify clerkWebhookVerify) {
        this.userRepository = userRepository;
        this.clerkWebhookVerify = clerkWebhookVerify;
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
                log.info("user created event called");

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
            log.info("user checking in db... for username-{} and email-{}",  req.getUsername(), req.getEmail());
            User user = userRepository.findByUsernameAndEmail(req.getUsername(), req.getEmail()).orElse(null);

            if(user == null) {
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


    public Response userProfile(HttpServletRequest request){
        try{

            

        }catch (Exception e){
            return new Response(
                    null,
                    e.getMessage(),
                    500,
                    e.toString()
            );
        }
    }

}

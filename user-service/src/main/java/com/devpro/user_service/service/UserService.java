package com.devpro.user_service.service;

import com.devpro.user_service.model.User;
import com.devpro.user_service.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.util.stream.Collectors;

@Service
public class UserService {


    private final UserRepository userRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ClerkWebhookVerify clerkWebhookVerify;

    public UserService(UserRepository userRepository,  ClerkWebhookVerify clerkWebhookVerify) {
        this.userRepository = userRepository;
        this.clerkWebhookVerify = clerkWebhookVerify;
    }


    public ResponseEntity<User> CreateUser(HttpServletRequest request) {

        try {
            String payload = request.getReader()
                    .lines()
                    .collect(Collectors.joining());

            clerkWebhookVerify.verify(request, payload);

            JsonNode root = objectMapper.readTree(payload);
            String eventType = root.get("type").asText();
            JsonNode data = root.get("data");

            if ("user.created".equals(eventType)) {

                String userId = data.get("id").asText();
                String primaryEmailId = data.get("primary_email_address_id").asText();

                String email = null;
                for (JsonNode e : data.get("email_addresses")) {
                    if (e.get("id").asText().equals(primaryEmailId)) {
                        email = e.get("email_address").asText();
                        break;
                    }
                }

                if (email == null) {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
                }

                User user = new User();
                user.setId(userId);
                user.setEmail(email);

                userRepository.save(user);

                return ResponseEntity.ok(user);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return null;
    }


}

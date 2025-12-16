package com.devpro.user_service.service;

import com.svix.Webhook;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.http.HttpHeaders;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ClerkWebhookVerify {
    @Value("${CLERK_TOKEN}")
    private  String CLERK_WEBHOOK_SECRET;

    public  void verify(HttpServletRequest request, String payload) throws Exception {
        if(CLERK_WEBHOOK_SECRET == null) {
            throw new RuntimeException("CLERK_WEBHOOK_SECRET not set");
        }
        Map<String, List<String>> headerMap = new HashMap<>();
        request.getHeaderNames().asIterator().forEachRemaining(
                name -> headerMap.put(name, List.of(request.getHeader(name)))
        );
        HttpHeaders headers = HttpHeaders.of(headerMap, (k, v) -> true);

        Webhook webhook = new Webhook(CLERK_WEBHOOK_SECRET);

        webhook.verify(
                payload,
               headers
        );
    }
}

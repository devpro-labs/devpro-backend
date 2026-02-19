package com.devpro.code_runner_service.config.socket_configs;

import com.devpro.code_runner_service.DTO.CustomResponse;
import com.devpro.code_runner_service.DTO.ExecutionEvent;
import com.devpro.code_runner_service.DTO.SocketEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class LogWebSocketHandler extends TextWebSocketHandler {
    // submissionId → session
    private  final Map<String, WebSocketSession> sessions = new HashMap<>();

    // submissionId → containerId
    private  final Map<String, String> containers = new HashMap<>();

    private    final RedisTemplate<String, String> redisTemplate;

    private  final ObjectMapper objectMapper;

    public LogWebSocketHandler(RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }


    public  void bindSession(String submissionId, WebSocketSession session) {

        if (submissionId == null || session == null) {
            return;
        }

        sessions.put(submissionId, session);

        System.out.println("SESSION BINDED: " + submissionId);

        // Optional: Send connected event to frontend
       sendEvent(submissionId, "CONNECTED", new CustomResponse(null, "Connected", 200, "Connected"));
    }


    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {

        String executionId = extractSubmissionId(session);
        sessions.put(executionId, session);

        System.out.println("WS CONNECTED: " + executionId);

        bindSession(executionId, session);

        String eventKey = "execution:" + executionId + ":events";

        List<String> pendingEvents =
                redisTemplate.opsForList().range(eventKey, 0, -1);

        if (pendingEvents != null) {
            for (String json : pendingEvents) {
                session.sendMessage(new TextMessage(json));
            }

            // clear after sending
            redisTemplate.delete(eventKey);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // Optional: client → server messages
    }

    @Override
    public void afterConnectionClosed(
            WebSocketSession session,
            CloseStatus status
    ) {

        String submissionId = extractSubmissionId(session);

        sessions.remove(submissionId);

        // Kill container on disconnect
        String containerId = containers.remove(submissionId);
        if (containerId != null) {
            killContainer(containerId);
        }

        System.out.println("WS CLOSED: " + submissionId);
    }

    // -------------------------
    // Utility methods
    // -------------------------

    public  WebSocketSession getSession(String submissionId) {
        return sessions.get(submissionId);
    }

    public  void removeSession(String executionId) {
        sessions.remove(executionId);
    }


    public  void bindContainer(String submissionId, String containerId) {
        containers.put(submissionId, containerId);
    }

    public  void removeContainer(String executionId) {
        containers.remove(executionId);
    }

    private  String extractSubmissionId(WebSocketSession session) {
        String path = session.getUri().getPath();
        return path.substring(path.lastIndexOf("/") + 1);
    }

    private  void killContainer(String containerId) {
        // dockerClient.killContainerCmd(containerId).exec();
        System.out.println("KILLING CONTAINER: " + containerId);
    }


    public  void sendEvent(String executionId, String type, Object data) {

        WebSocketSession session = sessions.get(executionId);

        if (session != null && session.isOpen()) {
            try {
                ExecutionEvent event = new ExecutionEvent(type, data);
                String json = objectMapper.writeValueAsString(event);

                session.sendMessage(new TextMessage(json));

                log.debug("Event sent | executionId={} | type={}",
                        executionId, type);

            } catch (Exception e) {
                log.error("Failed to send event | executionId={} | error={}",
                        executionId, e.getMessage());
            }
        } else {
            log.warn("No active session found | executionId={}", executionId);
            //add into queue
            try{
                redisTemplate.opsForList()
                        .leftPush("execution:" +executionId +":events"
                        , objectMapper.writeValueAsString(
                                new SocketEvent(
                                        executionId,
                                        type,
                                        data
                                )
                        ) );
            } catch (Exception e) {
                log.info("error at push message{}", e.getMessage());
            }

        }
    }
    // Cleanup

    public  void cleanup(String executionId) {
        removeSession(executionId);
        removeContainer(executionId);
    }
}

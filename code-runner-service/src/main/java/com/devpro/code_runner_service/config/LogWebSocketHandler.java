package com.devpro.code_runner_service.config;

import com.devpro.code_runner_service.DTO.CustomResponse;
import com.devpro.code_runner_service.DTO.ExecutionEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LogWebSocketHandler extends TextWebSocketHandler {
    // submissionId → session
    private static final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    // submissionId → containerId
    private static final Map<String, String> containers = new ConcurrentHashMap<>();

    private static final Logger log =
            LoggerFactory.getLogger(LogWebSocketHandler.class);

    private static ObjectMapper objectMapper = new ObjectMapper();

    private final ExecutionRegistry executionRegistry;

    public LogWebSocketHandler(ExecutionRegistry executionRegistry) {
        this.executionRegistry = executionRegistry;
    }

    public static void bindSession(String submissionId, WebSocketSession session) {

        if (submissionId == null || session == null) {
            return;
        }

        sessions.put(submissionId, session);

        System.out.println("SESSION BINDED: " + submissionId);

        // Optional: Send connected event to frontend
        sendMessage(submissionId, "CONNECTED");
    }


    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {

        String submissionId = extractSubmissionId(session);
        sessions.put(submissionId, session);

        System.out.println("WS CONNECTED: " + submissionId);

        bindSession(submissionId, session);

        // 🔥 Start execution only AFTER socket is connected
        executionRegistry.startExecution(submissionId);
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

    public static WebSocketSession getSession(String submissionId) {
        return sessions.get(submissionId);
    }

    public static void removeSession(String executionId) {
        sessions.remove(executionId);
    }


    public static void bindContainer(String submissionId, String containerId) {
        containers.put(submissionId, containerId);
    }

    public static void removeContainer(String executionId) {
        containers.remove(executionId);
    }

    private static String extractSubmissionId(WebSocketSession session) {
        String path = session.getUri().getPath();
        return path.substring(path.lastIndexOf("/") + 1);
    }

    private static void killContainer(String containerId) {
        // dockerClient.killContainerCmd(containerId).exec();
        System.out.println("KILLING CONTAINER: " + containerId);
    }

    public static void sendMessage(String executionId, String message) {

        WebSocketSession session = sessions.get(executionId);

        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(message));
            } catch (Exception ignored) {}
        }
    }

    public static void sendEvent(String executionId, String type, Object data) {

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
        }
    }

    public static void sendData(String executionId, CustomResponse response) {

        WebSocketSession session = sessions.get(executionId);

        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(response.toString()));
            } catch (Exception ignored) {}
        }
    }



    public static void sendError(String executionId, String error) {
        sendMessage(executionId, "ERROR: " + error);
    }

    // Cleanup

    public static void cleanup(String executionId) {
        removeSession(executionId);
        removeContainer(executionId);
    }
}

package com.eduquest.api.realtime;

import com.eduquest.api.repository.PrivateGroupRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatSocketHandler extends TextWebSocketHandler implements PresenceService {

    public static final String USER_ID = "userId";
    public static final String USERNAME = "username";

    private static final Logger log = LoggerFactory.getLogger(ChatSocketHandler.class);
    private static final int SEND_TIME_LIMIT_MS = 5000;
    private static final int BUFFER_LIMIT_BYTES = 256 * 1024;

    private final Map<Long, Map<String, WebSocketSession>> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final PrivateGroupRepository groupRepository;

    public ChatSocketHandler(ObjectMapper objectMapper, PrivateGroupRepository groupRepository) {
        this.objectMapper = objectMapper;
        this.groupRepository = groupRepository;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = userIdOf(session);
        if (userId == null) {
            return;
        }
        sessions.computeIfAbsent(userId, id -> new ConcurrentHashMap<>())
                .put(session.getId(), new ConcurrentWebSocketSessionDecorator(session, SEND_TIME_LIMIT_MS, BUFFER_LIMIT_BYTES));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = userIdOf(session);
        if (userId == null) {
            return;
        }
        sessions.computeIfPresent(userId, (id, userSessions) -> {
            userSessions.remove(session.getId());
            return userSessions.isEmpty() ? null : userSessions;
        });
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        Long userId = userIdOf(session);
        if (userId == null) {
            return;
        }
        Map<?, ?> body;
        try {
            body = objectMapper.readValue(message.getPayload(), Map.class);
        } catch (RuntimeException ex) {
            return;
        }
        String type = String.valueOf(body.get("type"));
        if ("ping".equals(type)) {
            sendToUsers(List.of(userId), Map.of("type", "pong"));
        } else if ("typing".equals(type)) {
            relayTyping(userId, String.valueOf(session.getAttributes().get(USERNAME)), body);
        }
    }

    public void sendToUsers(Collection<Long> userIds, Object payload) {
        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (RuntimeException ex) {
            log.warn("No se pudo serializar el mensaje en tiempo real", ex);
            return;
        }
        TextMessage text = new TextMessage(json);
        for (Long userId : userIds) {
            Map<String, WebSocketSession> userSessions = sessions.get(userId);
            if (userSessions == null) {
                continue;
            }
            for (WebSocketSession session : userSessions.values()) {
                send(session, text);
            }
        }
    }

    @Override
    public boolean isOnline(Long userId) {
        return sessions.containsKey(userId);
    }

    private void relayTyping(Long userId, String username, Map<?, ?> body) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "typing");
        payload.put("userId", userId);
        payload.put("username", username);
        Long groupId = asLong(body.get("groupId"));
        Long recipientId = asLong(body.get("userId"));
        if (groupId != null) {
            if (!groupRepository.existsByIdAndMembersId(groupId, userId)) {
                return;
            }
            payload.put("groupId", groupId);
            List<Long> others = groupRepository.findMemberIds(groupId).stream()
                    .filter(memberId -> !memberId.equals(userId))
                    .toList();
            sendToUsers(others, payload);
        } else if (recipientId != null && !recipientId.equals(userId)) {
            sendToUsers(List.of(recipientId), payload);
        }
    }

    private static void send(WebSocketSession session, TextMessage text) {
        if (!session.isOpen()) {
            return;
        }
        try {
            session.sendMessage(text);
        } catch (IOException | IllegalStateException ex) {
            log.debug("No se pudo enviar al socket {}", session.getId());
        }
    }

    private static Long userIdOf(WebSocketSession session) {
        return session.getAttributes().get(USER_ID) instanceof Long id ? id : null;
    }

    private static Long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && text.matches("\\d{1,18}")) {
            return Long.parseLong(text);
        }
        return null;
    }
}

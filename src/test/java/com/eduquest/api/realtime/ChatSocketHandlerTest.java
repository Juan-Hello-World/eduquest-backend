package com.eduquest.api.realtime;

import com.eduquest.api.repository.PrivateGroupRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatSocketHandlerTest {

    private final ObjectMapper objectMapper = JsonMapper.builder().build();
    private final PrivateGroupRepository groupRepository = mock(PrivateGroupRepository.class);
    private ChatSocketHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ChatSocketHandler(objectMapper, groupRepository);
    }

    private WebSocketSession session(String id, Long userId, String username) {
        WebSocketSession session = mock(WebSocketSession.class);
        Map<String, Object> attributes = new HashMap<>();
        if (userId != null) {
            attributes.put(ChatSocketHandler.USER_ID, userId);
            attributes.put(ChatSocketHandler.USERNAME, username);
        }
        when(session.getId()).thenReturn(id);
        when(session.getAttributes()).thenReturn(attributes);
        when(session.isOpen()).thenReturn(true);
        return session;
    }

    private String lastSent(WebSocketSession session) throws Exception {
        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session).sendMessage(captor.capture());
        return captor.getValue().getPayload();
    }

    @Test
    void sendToUsers_deliversOnlyToConnectedUsers() throws Exception {
        WebSocketSession alice = session("a", 1L, "alice");
        WebSocketSession bob = session("b", 2L, "bob");
        handler.afterConnectionEstablished(alice);
        handler.afterConnectionEstablished(bob);

        handler.sendToUsers(List.of(1L, 3L), Map.of("type", "group.message"));

        assertThat(lastSent(alice)).contains("group.message");
        verify(bob, never()).sendMessage(any());
        assertThat(handler.isOnline(1L)).isTrue();

        handler.afterConnectionClosed(alice, CloseStatus.NORMAL);
        assertThat(handler.isOnline(1L)).isFalse();
    }

    @Test
    void ping_repliesWithPong() throws Exception {
        WebSocketSession alice = session("a", 1L, "alice");
        handler.afterConnectionEstablished(alice);

        handler.handleTextMessage(alice, new TextMessage("{\"type\":\"ping\"}"));

        assertThat(lastSent(alice)).contains("pong");
    }

    @Test
    void typingInGroup_reachesOtherMembersOnly() throws Exception {
        WebSocketSession alice = session("a", 1L, "alice");
        WebSocketSession bob = session("b", 2L, "bob");
        handler.afterConnectionEstablished(alice);
        handler.afterConnectionEstablished(bob);
        when(groupRepository.existsByIdAndMembersId(5L, 1L)).thenReturn(true);
        when(groupRepository.findMemberIds(5L)).thenReturn(List.of(1L, 2L));

        handler.handleTextMessage(alice, new TextMessage("{\"type\":\"typing\",\"groupId\":5}"));

        assertThat(lastSent(bob)).contains("\"groupId\":5").contains("alice");
        verify(alice, never()).sendMessage(any());
    }

    @Test
    void typingInGroup_ignoredForNonMembers_andDirectTypingReachesRecipient() throws Exception {
        WebSocketSession alice = session("a", 1L, "alice");
        WebSocketSession bob = session("b", 2L, "bob");
        handler.afterConnectionEstablished(alice);
        handler.afterConnectionEstablished(bob);
        when(groupRepository.existsByIdAndMembersId(5L, 1L)).thenReturn(false);

        handler.handleTextMessage(alice, new TextMessage("{\"type\":\"typing\",\"groupId\":5}"));
        verify(bob, never()).sendMessage(any());

        handler.handleTextMessage(alice, new TextMessage("{\"type\":\"typing\",\"userId\":\"2\"}"));
        assertThat(lastSent(bob)).contains("\"userId\":1");
    }

    @Test
    void invalidPayloadsAndAnonymousSessions_areIgnored() throws Exception {
        WebSocketSession anonymous = session("x", null, null);
        handler.afterConnectionEstablished(anonymous);
        handler.handleTextMessage(anonymous, new TextMessage("{\"type\":\"ping\"}"));
        handler.afterConnectionClosed(anonymous, CloseStatus.NORMAL);

        WebSocketSession alice = session("a", 1L, "alice");
        handler.afterConnectionEstablished(alice);
        handler.handleTextMessage(alice, new TextMessage("no es json"));
        handler.handleTextMessage(alice, new TextMessage("{\"type\":\"typing\",\"userId\":1}"));

        verify(anonymous, never()).sendMessage(any());
        verify(alice, never()).sendMessage(any());
    }

    @Test
    void closedSessions_areSkipped() throws Exception {
        WebSocketSession alice = session("a", 1L, "alice");
        when(alice.isOpen()).thenReturn(false);
        handler.afterConnectionEstablished(alice);

        handler.sendToUsers(List.of(1L), Map.of("type", "x"));

        verify(alice, never()).sendMessage(any());
    }
}

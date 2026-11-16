package com.eduquest.api.realtime;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RealtimeListenerTest {

    @Test
    void handle_forwardsPayloadToSocketHandler() {
        ChatSocketHandler handler = mock(ChatSocketHandler.class);
        RealtimeListener listener = new RealtimeListener(handler);
        Map<String, Object> payload = Map.of("type", "group.message");

        listener.handle(new RealtimeEvent(this, List.of(1L, 2L), payload));

        verify(handler).sendToUsers(List.of(1L, 2L), payload);
    }
}

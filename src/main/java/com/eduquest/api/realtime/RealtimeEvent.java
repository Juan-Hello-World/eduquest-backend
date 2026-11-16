package com.eduquest.api.realtime;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Getter
public class RealtimeEvent extends ApplicationEvent {
    private final List<Long> userIds;
    private final Map<String, Object> payload;
    private final boolean broadcast;

    public RealtimeEvent(Object source, Collection<Long> userIds, Map<String, Object> payload) {
        this(source, userIds, payload, false);
    }

    private RealtimeEvent(Object source, Collection<Long> userIds, Map<String, Object> payload, boolean broadcast) {
        super(source);
        this.userIds = List.copyOf(userIds);
        this.payload = payload;
        this.broadcast = broadcast;
    }

    public static RealtimeEvent toEveryone(Object source, Map<String, Object> payload) {
        return new RealtimeEvent(source, List.of(), payload, true);
    }
}

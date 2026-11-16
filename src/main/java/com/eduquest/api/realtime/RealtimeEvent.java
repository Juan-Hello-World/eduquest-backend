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

    public RealtimeEvent(Object source, Collection<Long> userIds, Map<String, Object> payload) {
        super(source);
        this.userIds = List.copyOf(userIds);
        this.payload = payload;
    }
}

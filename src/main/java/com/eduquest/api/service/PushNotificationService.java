package com.eduquest.api.service;

import com.eduquest.api.event.OnPushEvent;

import java.util.List;

public interface PushNotificationService {
    void register(String username, String token);
    void unregister(String username, String token);
    void send(List<OnPushEvent.PushMessage> messages);
}

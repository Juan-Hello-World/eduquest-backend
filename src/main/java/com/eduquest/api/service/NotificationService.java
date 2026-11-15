package com.eduquest.api.service;

import com.eduquest.api.dto.request.AnnouncementRequestDTO;
import com.eduquest.api.dto.response.NotificationResponseDTO;
import com.eduquest.api.dto.response.PageResponseDTO;
import com.eduquest.api.entity.NotificationType;
import com.eduquest.api.entity.User;
import org.springframework.data.domain.Pageable;

public interface NotificationService {
    PageResponseDTO<NotificationResponseDTO> getNotifications(String username, Pageable pageable);
    long countUnread(String username);
    void markRead(Long notificationId, String username);
    void markAllRead(String username);
    int announce(AnnouncementRequestDTO request);
    void notifyGroupActivity(Long groupId, Long actorId, NotificationType type, String subject);
    void notifyDirectMessage(Long senderId, Long recipientId, String preview);
    void notifyBadge(User user, String badgeName, String badgeDescription);
    void markConversationRead(Long recipientId, Long senderId);
}

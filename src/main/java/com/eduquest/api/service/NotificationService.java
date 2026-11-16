package com.eduquest.api.service;

import com.eduquest.api.dto.request.AnnouncementRequestDTO;
import com.eduquest.api.dto.response.NotificationResponseDTO;
import com.eduquest.api.dto.response.PageResponseDTO;
import com.eduquest.api.entity.GroupChannel;
import com.eduquest.api.entity.NotificationType;
import com.eduquest.api.entity.PrivateGroup;
import com.eduquest.api.entity.User;
import org.springframework.data.domain.Pageable;

public interface NotificationService {
    PageResponseDTO<NotificationResponseDTO> getNotifications(String username, Pageable pageable);
    long countUnread(String username);
    void markRead(Long notificationId, String username);
    void markAllRead(String username);
    int announce(AnnouncementRequestDTO request);
    void notifyGroupActivity(Long groupId, Long actorId, NotificationType type, String subject);
    void notifyFollowers(Long authorId, String documentTitle);
    void notifyDirectMessage(Long senderId, Long recipientId, String preview);
    void notifyBadge(User user, String badgeName, String badgeDescription);
    void notifyWarning(User user, String message);
    void notifyForumActivity(User recipient, String title, String questionTitle, Long questionId);
    void notifyAppealDecision(User user, boolean accepted, String response);
    void notifyMention(User recipient, User author, PrivateGroup group, GroupChannel channel, Long messageId, String preview);
    void notifyRoomMessage(User recipient, User author, Long roomId, String roomName, String preview);
    void markRoomRead(Long recipientId, Long roomId);
    void notifyReply(User recipient, User author, PrivateGroup group, GroupChannel channel, Long messageId, String preview);
    void markConversationRead(Long recipientId, Long senderId);
}

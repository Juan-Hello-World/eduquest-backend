package com.eduquest.api.service.impl;

import com.eduquest.api.dto.request.AnnouncementRequestDTO;
import com.eduquest.api.dto.response.NotificationResponseDTO;
import com.eduquest.api.dto.response.PageResponseDTO;
import com.eduquest.api.entity.GroupChannel;
import com.eduquest.api.entity.Notification;
import com.eduquest.api.entity.NotificationType;
import com.eduquest.api.entity.PrivateGroup;
import com.eduquest.api.entity.User;
import com.eduquest.api.event.OnPushEvent;
import com.eduquest.api.exception.ResourceNotFoundException;
import com.eduquest.api.repository.GroupVisitRepository;
import com.eduquest.api.repository.NotificationRepository;
import com.eduquest.api.repository.PrivateGroupRepository;
import com.eduquest.api.repository.UserRepository;
import com.eduquest.api.service.NotificationService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class NotificationServiceImpl implements NotificationService {

    private static final int PREVIEW_LENGTH = 120;

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final PrivateGroupRepository groupRepository;
    private final GroupVisitRepository visitRepository;
    private final ApplicationEventPublisher eventPublisher;

    public NotificationServiceImpl(NotificationRepository notificationRepository,
                                   UserRepository userRepository,
                                   PrivateGroupRepository groupRepository,
                                   GroupVisitRepository visitRepository,
                                   ApplicationEventPublisher eventPublisher) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
        this.visitRepository = visitRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<NotificationResponseDTO> getNotifications(String username, Pageable pageable) {
        User user = findUser(username);
        Page<Notification> page = notificationRepository.findByRecipientIdOrderByCreatedAtDescIdDesc(user.getId(), pageable);
        List<NotificationResponseDTO> content = page.getContent().stream().map(this::toResponse).toList();
        return PageResponseDTO.of(content, page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnread(String username) {
        return notificationRepository.countByRecipientIdAndReadFalse(findUser(username).getId());
    }

    @Override
    @Transactional
    public void markRead(Long notificationId, String username) {
        Notification notification = notificationRepository.findByIdAndRecipientId(notificationId, findUser(username).getId())
                .orElseThrow(() -> new ResourceNotFoundException("Notificación no encontrada"));
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void markAllRead(String username) {
        notificationRepository.markAllRead(findUser(username).getId());
    }

    @Override
    @Transactional
    public int announce(AnnouncementRequestDTO request) {
        List<User> recipients;
        if (request.getUsername() != null && !request.getUsername().isBlank()) {
            String username = request.getUsername().trim().replaceFirst("^@", "");
            recipients = List.of(userRepository.findByUsername(username)
                    .orElseThrow(() -> new ResourceNotFoundException("No existe el usuario @" + username)));
        } else {
            recipients = userRepository.findActive();
        }
        String title = request.getTitle().trim();
        String body = request.getMessage().trim();
        List<Notification> notifications = recipients.stream()
                .map(user -> build(user, NotificationType.ANNOUNCEMENT, title, body, null, null))
                .toList();
        persistAll(notifications);
        return notifications.size();
    }

    @Override
    @Transactional
    public void notifyGroupActivity(Long groupId, Long actorId, NotificationType type, String subject) {
        PrivateGroup group = groupRepository.findById(groupId).orElse(null);
        User actor = userRepository.findById(actorId).orElse(null);
        if (group == null || actor == null) {
            return;
        }

        String title;
        String body;
        String link;
        switch (type) {
            case GROUP_MEMBER -> {
                title = "Nuevo integrante en " + group.getName();
                body = "@" + actor.getUsername() + " se unió al grupo.";
                link = "/groups/" + groupId + "?tab=members";
            }
            case GROUP_SESSION -> {
                title = "Nueva asesoría en " + group.getName();
                body = "@" + actor.getUsername() + " agendó «" + subject + "».";
                link = "/groups/" + groupId + "?tab=sessions";
            }
            default -> {
                title = "Nuevo material en " + group.getName();
                body = "@" + actor.getUsername() + " compartió «" + subject + "».";
                link = "/groups/" + groupId;
            }
        }

        Set<Long> muted = new HashSet<>(visitRepository.findMutedUserIds(groupId));
        List<Notification> notifications = group.getMembers().stream()
                .filter(member -> !member.getId().equals(actorId))
                .filter(member -> !muted.contains(member.getId()))
                .map(member -> build(member, type, title, body, link, null))
                .toList();
        persistAll(notifications);
    }

    @Override
    @Transactional
    public void notifyMention(User recipient, User author, PrivateGroup group, GroupChannel channel, Long messageId,
                              String preview) {
        String title = "@" + author.getUsername() + " te mencionó en " + group.getName();
        persist(build(recipient, NotificationType.ANNOUNCEMENT, title, "#" + channel.getName() + ": " + abbreviate(preview),
                messageLink(group, channel, messageId), null));
    }

    @Override
    @Transactional
    public void notifyRoomMessage(User recipient, User author, Long roomId, String roomName, String preview) {
        String collapseKey = "room:" + roomId;
        String body = "@" + author.getUsername() + ": " + abbreviate(preview);
        Notification notification = notificationRepository
                .findFirstByRecipientIdAndCollapseKeyAndReadFalse(recipient.getId(), collapseKey)
                .orElseGet(() -> build(recipient, NotificationType.DIRECT_MESSAGE, "Nuevos mensajes en " + roomName, body,
                        "/messages/rooms/" + roomId, collapseKey));
        notification.setBody(body);
        notification.setCreatedAt(LocalDateTime.now());
        persist(notification);
    }

    @Override
    @Transactional
    public void markRoomRead(Long recipientId, Long roomId) {
        notificationRepository.markReadByCollapseKey(recipientId, "room:" + roomId);
    }

    @Override
    @Transactional
    public void notifyReply(User recipient, User author, PrivateGroup group, GroupChannel channel, Long messageId,
                            String preview) {
        String title = "@" + author.getUsername() + " respondió a tu mensaje en " + group.getName();
        persist(build(recipient, NotificationType.ANNOUNCEMENT, title, "#" + channel.getName() + ": " + abbreviate(preview),
                messageLink(group, channel, messageId), null));
    }

    private static String messageLink(PrivateGroup group, GroupChannel channel, Long messageId) {
        return "/groups/" + group.getId() + "?tab=chat&channel=" + channel.getId() + "&message=" + messageId;
    }

    @Override
    @Transactional
    public void notifyFollowers(Long authorId, String documentTitle) {
        User author = userRepository.findById(authorId).orElse(null);
        if (author == null) {
            return;
        }
        String title = "@" + author.getUsername() + " compartió material nuevo";
        String body = "«" + abbreviate(documentTitle) + "» ya está en la biblioteca.";
        List<Notification> notifications = userRepository.findFollowers(authorId).stream()
                .map(follower -> build(follower, NotificationType.GROUP_DOCUMENT, title, body,
                        "/users/" + authorId, null))
                .toList();
        persistAll(notifications);
    }

    @Override
    @Transactional
    public void notifyDirectMessage(Long senderId, Long recipientId, String preview) {
        User sender = userRepository.findById(senderId).orElse(null);
        User recipient = userRepository.findById(recipientId).orElse(null);
        if (sender == null || recipient == null) {
            return;
        }
        String collapseKey = "dm:" + senderId;
        String body = abbreviate(preview);
        Notification notification = notificationRepository
                .findFirstByRecipientIdAndCollapseKeyAndReadFalse(recipientId, collapseKey)
                .orElseGet(() -> build(recipient, NotificationType.DIRECT_MESSAGE,
                        "Mensaje de @" + sender.getUsername(), body, "/messages/" + senderId, collapseKey));
        notification.setBody(body);
        notification.setCreatedAt(LocalDateTime.now());
        persist(notification);
    }

    @Override
    @Transactional
    public void notifyBadge(User user, String badgeName, String badgeDescription) {
        persist(build(user, NotificationType.BADGE,
                "¡Desbloqueaste la insignia " + badgeName + "!", badgeDescription, "/achievements", null));
    }

    @Override
    @Transactional
    public void notifyWarning(User user, String message) {
        persist(build(user, NotificationType.ANNOUNCEMENT,
                "Advertencia de la moderación", message.trim(), null, null));
    }

    @Override
    @Transactional
    public void notifyForumActivity(User recipient, String title, String questionTitle, Long questionId) {
        persist(build(recipient, NotificationType.ANNOUNCEMENT, title,
                "«" + abbreviate(questionTitle) + "»", "/forum/" + questionId, null));
    }

    @Override
    @Transactional
    public void notifyAppealDecision(User user, boolean accepted, String response) {
        String title = accepted ? "Aceptamos tu apelación" : "Revisamos tu apelación";
        String body = accepted
                ? "Tu suspensión fue levantada. Recuerda respetar las normas de la comunidad."
                : "La suspensión se mantiene.";
        if (response != null && !response.isBlank()) {
            body += " Respuesta de la moderación: " + response.trim();
        }
        persist(build(user, NotificationType.ANNOUNCEMENT, title, body, null, null));
    }

    @Override
    @Transactional
    public void markConversationRead(Long recipientId, Long senderId) {
        notificationRepository.markReadByCollapseKey(recipientId, "dm:" + senderId);
    }

    private void persist(Notification notification) {
        notificationRepository.save(notification);
        publishPush(List.of(notification));
    }

    private void persistAll(List<Notification> notifications) {
        if (notifications.isEmpty()) {
            return;
        }
        notificationRepository.saveAll(notifications);
        publishPush(notifications);
    }

    private void publishPush(List<Notification> notifications) {
        eventPublisher.publishEvent(new OnPushEvent(this, notifications.stream()
                .map(notification -> new OnPushEvent.PushMessage(notification.getRecipient().getId(),
                        notification.getTitle(), notification.getBody(), notification.getLink()))
                .toList()));
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    private static String abbreviate(String text) {
        if (text == null) {
            return "";
        }
        String clean = text.trim();
        return clean.length() <= PREVIEW_LENGTH ? clean : clean.substring(0, PREVIEW_LENGTH - 1) + "…";
    }

    private static Notification build(User recipient, NotificationType type, String title, String body,
                                      String link, String collapseKey) {
        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setType(type);
        notification.setTitle(title.length() > 150 ? title.substring(0, 149) + "…" : title);
        notification.setBody(body);
        notification.setLink(link);
        notification.setCollapseKey(collapseKey);
        notification.setCreatedAt(LocalDateTime.now());
        return notification;
    }

    private NotificationResponseDTO toResponse(Notification notification) {
        return new NotificationResponseDTO(notification.getId(), notification.getType().name(), notification.getTitle(),
                notification.getBody(), notification.getLink(), notification.isRead(), notification.getCreatedAt());
    }
}

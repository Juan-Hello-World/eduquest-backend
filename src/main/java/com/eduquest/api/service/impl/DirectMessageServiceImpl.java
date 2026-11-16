package com.eduquest.api.service.impl;

import com.eduquest.api.dto.request.ChatMessages;
import com.eduquest.api.dto.request.GroupMessageRequestDTO;
import com.eduquest.api.dto.response.ConversationResponseDTO;
import com.eduquest.api.dto.response.DirectMessageResponseDTO;
import com.eduquest.api.dto.response.MessageReplyDTO;
import com.eduquest.api.dto.response.UserResponseDTO;
import com.eduquest.api.dto.response.UserSummaryDTO;
import com.eduquest.api.entity.DirectMessage;
import com.eduquest.api.entity.User;
import com.eduquest.api.event.OnDirectMessageSentEvent;
import com.eduquest.api.exception.BadRequestException;
import com.eduquest.api.exception.ForbiddenException;
import com.eduquest.api.exception.InvalidOperationException;
import com.eduquest.api.exception.ResourceNotFoundException;
import com.eduquest.api.realtime.RealtimeEvent;
import com.eduquest.api.repository.DirectMessageRepository;
import com.eduquest.api.repository.UserRepository;
import com.eduquest.api.service.DirectMessageService;
import com.eduquest.api.service.FileStorageService;
import com.eduquest.api.service.NotificationService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DirectMessageServiceImpl implements DirectMessageService {

    private static final int PAGE_SIZE = 50;

    private final DirectMessageRepository messageRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final ApplicationEventPublisher eventPublisher;
    private final FileStorageService fileStorageService;

    public DirectMessageServiceImpl(DirectMessageRepository messageRepository,
                                    UserRepository userRepository,
                                    NotificationService notificationService,
                                    ApplicationEventPublisher eventPublisher,
                                    FileStorageService fileStorageService) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.eventPublisher = eventPublisher;
        this.fileStorageService = fileStorageService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConversationResponseDTO> getConversations(String username) {
        User me = findUser(username);
        List<ConversationResponseDTO> conversations = new ArrayList<>();
        for (Long partnerId : messageRepository.findPartnerIds(me.getId())) {
            User partner = userRepository.findById(partnerId).orElse(null);
            List<DirectMessage> latest = messageRepository.findLatest(me.getId(), partnerId, PageRequest.of(0, 1));
            if (partner == null || latest.isEmpty()) {
                continue;
            }
            DirectMessage last = latest.getFirst();
            conversations.add(new ConversationResponseDTO(
                    summary(partner),
                    last.isDeleted() ? ChatMessages.DELETED_PREVIEW : ChatMessages.preview(last.getContent(), last.getImageUrl()),
                    last.getSender().getId().equals(me.getId()),
                    last.getCreatedAt(),
                    messageRepository.countBySenderIdAndRecipientIdAndReadAtIsNull(partnerId, me.getId())));
        }
        conversations.sort(Comparator.comparing(ConversationResponseDTO::lastMessageAt).reversed());
        return conversations;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DirectMessageResponseDTO> getMessages(String username, Long otherUserId, Long after, Long before) {
        User me = findUser(username);
        findUser(otherUserId);
        PageRequest page = PageRequest.of(0, PAGE_SIZE);

        List<DirectMessage> messages;
        if (after != null) {
            messages = messageRepository.findAfter(me.getId(), otherUserId, after, PageRequest.of(0, 100));
        } else {
            messages = new ArrayList<>(before != null
                    ? messageRepository.findBefore(me.getId(), otherUserId, before, page)
                    : messageRepository.findLatest(me.getId(), otherUserId, page));
            Collections.reverse(messages);
        }
        return messages.stream().map(message -> toResponse(message, me)).toList();
    }

    @Override
    @Transactional
    public DirectMessageResponseDTO sendMessage(String username, Long otherUserId, GroupMessageRequestDTO request) {
        User me = findUser(username);
        User other = findUser(otherUserId);
        if (me.getId().equals(other.getId())) {
            throw new InvalidOperationException("No puedes enviarte mensajes a ti mismo");
        }
        if (other.isBanned()) {
            throw new InvalidOperationException("Esta cuenta está suspendida y no puede recibir mensajes");
        }

        DirectMessage message = new DirectMessage();
        message.setSender(me);
        message.setRecipient(other);
        boolean hasAttachment = ChatMessages.present(request.getImageUrl()) || ChatMessages.present(request.getFileUrl())
                || ChatMessages.present(request.getSticker());
        message.setContent(ChatMessages.cleanContent(request.getContent(), hasAttachment));
        message.setImageUrl(ChatMessages.cleanImage(request.getImageUrl()));
        message.setFileUrl(ChatMessages.present(request.getFileUrl()) ? request.getFileUrl().trim() : null);
        message.setFileName(ChatMessages.cleanFileName(request.getFileName(), message.getFileUrl()));
        message.setSticker(ChatMessages.present(request.getSticker()) ? request.getSticker().trim() : null);
        message.setCreatedAt(LocalDateTime.now());
        if (request.getReplyToId() != null) {
            message.setReplyTo(findMessage(me.getId(), other.getId(), request.getReplyToId()));
        }
        DirectMessage saved = messageRepository.save(message);

        eventPublisher.publishEvent(new OnDirectMessageSentEvent(this, me.getId(), other.getId(),
                ChatMessages.preview(saved.getContent(), saved.getImageUrl(), saved.getFileName(), saved.getSticker())));
        broadcast("created", saved);
        return toResponse(saved, me);
    }

    @Override
    @Transactional
    public DirectMessageResponseDTO editMessage(String username, Long otherUserId, Long messageId, String content) {
        User me = findUser(username);
        DirectMessage message = findMessage(me.getId(), otherUserId, messageId);
        if (!message.getSender().getId().equals(me.getId())) {
            throw new ForbiddenException("Solo puedes editar tus propios mensajes");
        }
        if (message.isDeleted()) {
            throw new BadRequestException("No puedes editar un mensaje eliminado");
        }
        boolean hasAttachment = message.getImageUrl() != null || message.getFileUrl() != null
                || message.getSticker() != null;
        message.setContent(ChatMessages.cleanContent(content, hasAttachment));
        message.setEditedAt(LocalDateTime.now());
        DirectMessage saved = messageRepository.save(message);
        broadcast("updated", saved);
        return toResponse(saved, me);
    }

    @Override
    @Transactional
    public DirectMessageResponseDTO deleteMessage(String username, Long otherUserId, Long messageId) {
        User me = findUser(username);
        DirectMessage message = findMessage(me.getId(), otherUserId, messageId);
        if (!message.getSender().getId().equals(me.getId())) {
            throw new ForbiddenException("Solo puedes eliminar tus propios mensajes");
        }
        if (!message.isDeleted()) {
            List<String> files = new ArrayList<>();
            if (message.getImageUrl() != null) {
                files.add(message.getImageUrl());
            }
            if (message.getFileUrl() != null) {
                files.add(message.getFileUrl());
            }
            message.setDeleted(true);
            message.setContent("");
            message.setImageUrl(null);
            message.setFileUrl(null);
            message.setFileName(null);
            message.setSticker(null);
            message = messageRepository.save(message);
            files.forEach(fileStorageService::deleteByUrl);
            broadcast("deleted", message);
        }
        return toResponse(message, me);
    }

    private DirectMessage findMessage(Long me, Long other, Long messageId) {
        return messageRepository.findById(messageId)
                .filter(message -> isBetween(message, me, other))
                .orElseThrow(() -> new ResourceNotFoundException("Mensaje no encontrado"));
    }

    private static boolean isBetween(DirectMessage message, Long me, Long other) {
        Long sender = message.getSender().getId();
        Long recipient = message.getRecipient().getId();
        return (sender.equals(me) && recipient.equals(other)) || (sender.equals(other) && recipient.equals(me));
    }

    private void broadcast(String action, DirectMessage message) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "direct.message");
        payload.put("action", action);
        payload.put("senderId", message.getSender().getId());
        payload.put("recipientId", message.getRecipient().getId());
        payload.put("message", toResponse(message, null));
        eventPublisher.publishEvent(new RealtimeEvent(this,
                List.of(message.getSender().getId(), message.getRecipient().getId()), payload));
    }

    @Override
    @Transactional
    public void markRead(String username, Long otherUserId) {
        User me = findUser(username);
        messageRepository.markRead(me.getId(), otherUserId, LocalDateTime.now());
        notificationService.markConversationRead(me.getId(), otherUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnread(String username) {
        return messageRepository.countByRecipientIdAndReadAtIsNull(findUser(username).getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSummaryDTO> searchUsers(String username, String query) {
        User me = findUser(username);
        String clean = query == null ? "" : query.trim().replaceFirst("^@", "").toLowerCase();
        if (clean.length() < 2) {
            return List.of();
        }
        String pattern = "%" + clean.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        return userRepository.searchActive(pattern, me.getId(), PageRequest.of(0, 10)).stream()
                .map(DirectMessageServiceImpl::summary)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UserSummaryDTO getUserSummary(Long userId) {
        return summary(findUser(userId));
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    private static UserSummaryDTO summary(User user) {
        return new UserSummaryDTO(user.getId(), user.getUsername(),
                UserResponseDTO.avatarUrlFor(user.getId(), user.getAvatarVersion()), user.isAdvisor());
    }

    private static DirectMessageResponseDTO toResponse(DirectMessage message, User viewer) {
        DirectMessage reply = message.getReplyTo();
        MessageReplyDTO replyTo = reply == null ? null : new MessageReplyDTO(reply.getId(), reply.getSender().getId(),
                reply.getSender().getUsername(),
                ChatMessages.replyPreview(reply.getContent(), reply.getImageUrl(), reply.getFileName(), reply.getSticker(),
                        reply.isDeleted()), reply.isDeleted());
        return new DirectMessageResponseDTO(message.getId(),
                message.isDeleted() ? "" : message.getContent(),
                message.isDeleted() ? null : message.getImageUrl(),
                message.isDeleted() ? null : message.getFileUrl(),
                message.isDeleted() ? null : message.getFileName(),
                message.isDeleted() ? null : message.getSticker(),
                message.getSender().getId(),
                viewer != null && message.getSender().getId().equals(viewer.getId()),
                message.getReadAt() != null, message.getCreatedAt(), message.getEditedAt(), message.isDeleted(), replyTo);
    }
}

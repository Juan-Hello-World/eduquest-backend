package com.eduquest.api.service.impl;

import com.eduquest.api.dto.response.ConversationResponseDTO;
import com.eduquest.api.dto.response.DirectMessageResponseDTO;
import com.eduquest.api.dto.response.UserResponseDTO;
import com.eduquest.api.dto.response.UserSummaryDTO;
import com.eduquest.api.entity.DirectMessage;
import com.eduquest.api.entity.User;
import com.eduquest.api.event.OnDirectMessageSentEvent;
import com.eduquest.api.exception.InvalidOperationException;
import com.eduquest.api.exception.ResourceNotFoundException;
import com.eduquest.api.repository.DirectMessageRepository;
import com.eduquest.api.repository.UserRepository;
import com.eduquest.api.service.DirectMessageService;
import com.eduquest.api.service.NotificationService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Service
public class DirectMessageServiceImpl implements DirectMessageService {

    private static final int PAGE_SIZE = 50;

    private final DirectMessageRepository messageRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final ApplicationEventPublisher eventPublisher;

    public DirectMessageServiceImpl(DirectMessageRepository messageRepository,
                                    UserRepository userRepository,
                                    NotificationService notificationService,
                                    ApplicationEventPublisher eventPublisher) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.eventPublisher = eventPublisher;
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
                    last.getContent(),
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
    public DirectMessageResponseDTO sendMessage(String username, Long otherUserId, String content) {
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
        message.setContent(content.trim());
        message.setCreatedAt(LocalDateTime.now());
        DirectMessage saved = messageRepository.save(message);

        eventPublisher.publishEvent(new OnDirectMessageSentEvent(this, me.getId(), other.getId(), saved.getContent()));
        return toResponse(saved, me);
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
        return new DirectMessageResponseDTO(message.getId(), message.getContent(), message.getSender().getId(),
                message.getSender().getId().equals(viewer.getId()), message.getReadAt() != null, message.getCreatedAt());
    }
}

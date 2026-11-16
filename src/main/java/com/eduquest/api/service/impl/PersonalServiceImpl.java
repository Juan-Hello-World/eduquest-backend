package com.eduquest.api.service.impl;

import com.eduquest.api.dto.request.PinnedChatRequestDTO;
import com.eduquest.api.dto.request.StatusRequestDTO;
import com.eduquest.api.dto.response.PinnedChatDTO;
import com.eduquest.api.dto.response.UserStatusDTO;
import com.eduquest.api.entity.Availability;
import com.eduquest.api.entity.PinnedChat;
import com.eduquest.api.entity.User;
import com.eduquest.api.exception.BadRequestException;
import com.eduquest.api.exception.InvalidOperationException;
import com.eduquest.api.exception.ResourceNotFoundException;
import com.eduquest.api.realtime.PresenceService;
import com.eduquest.api.repository.PinnedChatRepository;
import com.eduquest.api.repository.UserRepository;
import com.eduquest.api.service.PersonalService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PersonalServiceImpl implements PersonalService {

    static final int MAX_PINNED = 15;
    static final int MAX_STATUS_LOOKUP = 100;

    private final UserRepository userRepository;
    private final PinnedChatRepository pinnedChatRepository;
    private final PresenceService presenceService;

    public PersonalServiceImpl(UserRepository userRepository, PinnedChatRepository pinnedChatRepository,
                               PresenceService presenceService) {
        this.userRepository = userRepository;
        this.pinnedChatRepository = pinnedChatRepository;
        this.presenceService = presenceService;
    }

    @Override
    @Transactional
    public UserStatusDTO updateStatus(String username, StatusRequestDTO request) {
        User user = findUser(username);
        user.setAvailability(request.getAvailability());
        user.setStatusText(clean(request.getStatusText()));
        user.setStatusEmoji(clean(request.getStatusEmoji()));
        return toStatus(userRepository.save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserStatusDTO> getStatuses(List<Long> userIds) {
        if (userIds.size() > MAX_STATUS_LOOKUP) {
            throw new BadRequestException("Puedes consultar hasta " + MAX_STATUS_LOOKUP + " personas a la vez");
        }
        return userRepository.findAllById(userIds).stream().map(this::toStatus).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PinnedChatDTO> getPinnedChats(String username) {
        return pinnedChatRepository.findByUserIdOrderByPinnedAtAsc(findUser(username).getId()).stream()
                .map(pin -> new PinnedChatDTO(pin.getKind().name(), pin.getTargetId()))
                .toList();
    }

    @Override
    @Transactional
    public List<PinnedChatDTO> setPinned(String username, PinnedChatRequestDTO request) {
        User user = findUser(username);
        var existing = pinnedChatRepository.findByUserIdAndKindAndTargetId(user.getId(), request.getKind(), request.getTargetId());
        if (request.isPinned() && existing.isEmpty()) {
            if (pinnedChatRepository.countByUserId(user.getId()) >= MAX_PINNED) {
                throw new InvalidOperationException("Puedes fijar hasta " + MAX_PINNED + " chats");
            }
            PinnedChat pin = new PinnedChat();
            pin.setUser(user);
            pin.setKind(request.getKind());
            pin.setTargetId(request.getTargetId());
            pin.setPinnedAt(LocalDateTime.now());
            pinnedChatRepository.save(pin);
        } else if (!request.isPinned()) {
            existing.ifPresent(pinnedChatRepository::delete);
        }
        return getPinnedChats(username);
    }

    private UserStatusDTO toStatus(User user) {
        Availability availability = user.getAvailability() == null ? Availability.AVAILABLE : user.getAvailability();
        boolean online = availability != Availability.INVISIBLE && presenceService.isOnline(user.getId());
        String shownAvailability = availability == Availability.INVISIBLE ? Availability.AVAILABLE.name() : availability.name();
        return new UserStatusDTO(user.getId(), shownAvailability, user.getStatusText(), user.getStatusEmoji(), online);
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }
}

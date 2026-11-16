package com.eduquest.api.service.impl;

import com.eduquest.api.dto.request.StudyRoomRequestDTO;
import com.eduquest.api.dto.response.StudyRoomDTO;
import com.eduquest.api.dto.response.UserResponseDTO;
import com.eduquest.api.dto.response.UserSummaryDTO;
import com.eduquest.api.entity.PrivateGroup;
import com.eduquest.api.entity.RoleName;
import com.eduquest.api.entity.User;
import com.eduquest.api.exception.ForbiddenException;
import com.eduquest.api.exception.InvalidOperationException;
import com.eduquest.api.exception.ResourceNotFoundException;
import com.eduquest.api.realtime.RealtimeEvent;
import com.eduquest.api.repository.PrivateGroupRepository;
import com.eduquest.api.repository.UserRepository;
import com.eduquest.api.service.StudyRoomService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class StudyRoomServiceImpl implements StudyRoomService {

    private record Room(int focusMinutes, int breakMinutes, LocalDateTime startedAt, Long startedById, Set<Long> participants) {
    }

    private final Map<Long, Room> rooms = new ConcurrentHashMap<>();
    private final PrivateGroupRepository groupRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    public StudyRoomServiceImpl(PrivateGroupRepository groupRepository, UserRepository userRepository,
                                ApplicationEventPublisher eventPublisher) {
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional(readOnly = true)
    public StudyRoomDTO get(Long groupId, String username) {
        PrivateGroup group = findGroup(groupId);
        requireMember(group, findUser(username));
        return toDto(group, rooms.get(groupId));
    }

    @Override
    @Transactional(readOnly = true)
    public StudyRoomDTO start(Long groupId, StudyRoomRequestDTO request, String username) {
        PrivateGroup group = findGroup(groupId);
        User user = findUser(username);
        requireMember(group, user);
        if (rooms.containsKey(groupId)) {
            throw new InvalidOperationException("Ya hay una sala de estudio activa en este grupo");
        }
        Set<Long> participants = ConcurrentHashMap.newKeySet();
        participants.add(user.getId());
        rooms.put(groupId, new Room(request.getFocusMinutes(), request.getBreakMinutes(), LocalDateTime.now(),
                user.getId(), participants));
        return broadcast(group);
    }

    @Override
    @Transactional(readOnly = true)
    public StudyRoomDTO join(Long groupId, String username) {
        PrivateGroup group = findGroup(groupId);
        User user = findUser(username);
        requireMember(group, user);
        Room room = rooms.get(groupId);
        if (room == null) {
            throw new InvalidOperationException("No hay una sala de estudio activa; inicia una");
        }
        room.participants().add(user.getId());
        return broadcast(group);
    }

    @Override
    @Transactional(readOnly = true)
    public StudyRoomDTO leave(Long groupId, String username) {
        PrivateGroup group = findGroup(groupId);
        User user = findUser(username);
        requireMember(group, user);
        Room room = rooms.get(groupId);
        if (room != null) {
            room.participants().remove(user.getId());
            if (room.participants().isEmpty()) {
                rooms.remove(groupId);
            }
        }
        return broadcast(group);
    }

    @Override
    @Transactional(readOnly = true)
    public StudyRoomDTO stop(Long groupId, String username) {
        PrivateGroup group = findGroup(groupId);
        User user = findUser(username);
        requireMember(group, user);
        Room room = rooms.get(groupId);
        if (room != null) {
            boolean allowed = room.startedById().equals(user.getId()) || group.canManage(user) || user.hasRole(RoleName.ROLE_ADMIN);
            if (!allowed) {
                throw new ForbiddenException("Solo quien inició la sala o un moderador puede terminarla");
            }
            rooms.remove(groupId);
        }
        return broadcast(group);
    }

    private StudyRoomDTO broadcast(PrivateGroup group) {
        StudyRoomDTO dto = toDto(group, rooms.get(group.getId()));
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "group.studyroom");
        payload.put("groupId", group.getId());
        payload.put("room", dto);
        eventPublisher.publishEvent(new RealtimeEvent(this, group.getMembers().stream().map(User::getId).toList(), payload));
        return dto;
    }

    private static StudyRoomDTO toDto(PrivateGroup group, Room room) {
        if (room == null) {
            return new StudyRoomDTO(false, 0, 0, null, LocalDateTime.now(), null, List.of());
        }
        Set<Long> ids = new LinkedHashSet<>(room.participants());
        List<UserSummaryDTO> participants = group.getMembers().stream()
                .filter(member -> ids.contains(member.getId()))
                .map(member -> new UserSummaryDTO(member.getId(), member.getUsername(),
                        UserResponseDTO.avatarUrlFor(member.getId(), member.getAvatarVersion()), member.isAdvisor()))
                .toList();
        return new StudyRoomDTO(true, room.focusMinutes(), room.breakMinutes(), room.startedAt(), LocalDateTime.now(),
                room.startedById(), participants);
    }

    private void requireMember(PrivateGroup group, User user) {
        if (!groupRepository.existsByIdAndMembersId(group.getId(), user.getId())) {
            throw new ForbiddenException("Debes ser miembro del grupo");
        }
    }

    private PrivateGroup findGroup(Long groupId) {
        return groupRepository.findById(groupId).orElseThrow(() -> new ResourceNotFoundException("Grupo no encontrado"));
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }
}

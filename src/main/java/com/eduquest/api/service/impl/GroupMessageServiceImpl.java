package com.eduquest.api.service.impl;

import com.eduquest.api.dto.request.GroupMessageRequestDTO;
import com.eduquest.api.dto.response.GroupMessageResponseDTO;
import com.eduquest.api.dto.response.UserResponseDTO;
import com.eduquest.api.entity.GroupMessage;
import com.eduquest.api.entity.GroupVisit;
import com.eduquest.api.entity.PrivateGroup;
import com.eduquest.api.entity.User;
import com.eduquest.api.exception.ForbiddenException;
import com.eduquest.api.exception.ResourceNotFoundException;
import com.eduquest.api.repository.GroupMessageRepository;
import com.eduquest.api.repository.GroupVisitRepository;
import com.eduquest.api.repository.PrivateGroupRepository;
import com.eduquest.api.repository.UserRepository;
import com.eduquest.api.service.GroupMessageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class GroupMessageServiceImpl implements GroupMessageService {

    private final GroupMessageRepository messageRepository;
    private final PrivateGroupRepository groupRepository;
    private final UserRepository userRepository;
    private final GroupVisitRepository visitRepository;

    public GroupMessageServiceImpl(GroupMessageRepository messageRepository,
                                   PrivateGroupRepository groupRepository,
                                   UserRepository userRepository,
                                   GroupVisitRepository visitRepository) {
        this.messageRepository = messageRepository;
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
        this.visitRepository = visitRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupMessageResponseDTO> getMessages(Long groupId, String username, Long after, Long before) {
        User user = findUser(username);
        requireMember(groupId, user);

        List<GroupMessage> messages;
        if (after != null) {
            messages = messageRepository.findTop100ByGroupIdAndIdGreaterThanOrderByIdAsc(groupId, after);
        } else {
            messages = new ArrayList<>(before != null
                    ? messageRepository.findTop50ByGroupIdAndIdLessThanOrderByIdDesc(groupId, before)
                    : messageRepository.findTop50ByGroupIdOrderByIdDesc(groupId));
            Collections.reverse(messages);
        }
        return messages.stream().map(message -> toResponse(message, user)).toList();
    }

    @Override
    @Transactional
    public GroupMessageResponseDTO sendMessage(Long groupId, GroupMessageRequestDTO request, String username) {
        User user = findUser(username);
        PrivateGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Grupo no encontrado"));
        requireMember(groupId, user);

        GroupMessage message = new GroupMessage();
        message.setContent(request.getContent().trim());
        message.setGroup(group);
        message.setAuthor(user);
        message.setCreatedAt(LocalDateTime.now());
        return toResponse(messageRepository.save(message), user);
    }

    @Override
    @Transactional
    public void markChatSeen(Long groupId, String username) {
        User user = findUser(username);
        PrivateGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Grupo no encontrado"));
        requireMember(groupId, user);
        LocalDateTime now = LocalDateTime.now();
        GroupVisit visit = visitRepository.findByUserIdAndGroupId(user.getId(), groupId)
                .orElseGet(() -> new GroupVisit(user, group, now));
        visit.setChatSeenAt(now);
        visitRepository.save(visit);
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    private void requireMember(Long groupId, User user) {
        if (!groupRepository.existsByIdAndMembersId(groupId, user.getId())) {
            throw new ForbiddenException("Debes ser miembro del grupo para usar el chat");
        }
    }

    private GroupMessageResponseDTO toResponse(GroupMessage message, User viewer) {
        User author = message.getAuthor();
        GroupMessageResponseDTO dto = new GroupMessageResponseDTO();
        dto.setId(message.getId());
        dto.setContent(message.getContent());
        dto.setAuthorId(author.getId());
        dto.setAuthorUsername(author.getUsername());
        dto.setAuthorAvatarUrl(UserResponseDTO.avatarUrlFor(author.getId(), author.getAvatarVersion()));
        dto.setAuthorAdvisor(author.isAdvisor());
        dto.setMine(author.getId().equals(viewer.getId()));
        dto.setCreatedAt(message.getCreatedAt());
        return dto;
    }
}

package com.eduquest.api.service.impl;

import com.eduquest.api.dto.request.GroupRequestDTO;
import com.eduquest.api.dto.response.GroupInviteResponseDTO;
import com.eduquest.api.dto.response.GroupMemberResponseDTO;
import com.eduquest.api.dto.response.GroupResponseDTO;
import com.eduquest.api.dto.response.PageResponseDTO;
import com.eduquest.api.dto.response.UserResponseDTO;
import com.eduquest.api.entity.GroupVisibility;
import com.eduquest.api.entity.GroupVisit;
import com.eduquest.api.entity.NotificationType;
import com.eduquest.api.entity.PrivateGroup;
import com.eduquest.api.entity.User;
import com.eduquest.api.event.OnGroupActivityEvent;
import com.eduquest.api.exception.BadRequestException;
import com.eduquest.api.exception.DuplicateResourceException;
import com.eduquest.api.exception.ForbiddenException;
import com.eduquest.api.exception.InvalidOperationException;
import com.eduquest.api.exception.ResourceNotFoundException;
import com.eduquest.api.repository.DocumentRepository;
import com.eduquest.api.repository.GroupMessageRepository;
import com.eduquest.api.repository.GroupVisitRepository;
import com.eduquest.api.repository.PrivateGroupRepository;
import com.eduquest.api.repository.StudySessionRepository;
import com.eduquest.api.repository.UserRepository;
import com.eduquest.api.service.GroupCodeGenerator;
import com.eduquest.api.service.GroupService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GroupServiceImpl implements GroupService {

    private static final int INVITE_DAYS = 7;

    private final PrivateGroupRepository groupRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final GroupCodeGenerator codeGenerator;
    private final GroupVisitRepository visitRepository;
    private final DocumentRepository documentRepository;
    private final StudySessionRepository sessionRepository;
    private final GroupMessageRepository messageRepository;
    private final ApplicationEventPublisher eventPublisher;

    public GroupServiceImpl(PrivateGroupRepository groupRepository,
                            UserRepository userRepository,
                            PasswordEncoder passwordEncoder,
                            GroupCodeGenerator codeGenerator,
                            GroupVisitRepository visitRepository,
                            DocumentRepository documentRepository,
                            StudySessionRepository sessionRepository,
                            GroupMessageRepository messageRepository,
                            ApplicationEventPublisher eventPublisher) {
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.codeGenerator = codeGenerator;
        this.visitRepository = visitRepository;
        this.documentRepository = documentRepository;
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public GroupResponseDTO createGroup(GroupRequestDTO request, String username) {
        User user = findUser(username);
        GroupVisibility visibility = request.getVisibility() != null ? request.getVisibility() : GroupVisibility.PUBLIC;

        PrivateGroup group = new PrivateGroup();
        group.setName(request.getName());
        group.setDescription(request.getDescription());
        group.setCreator(user);
        group.setVisibility(visibility);
        group.setCode(codeGenerator.nextCode());
        group.setLastActivityAt(LocalDateTime.now());

        if (visibility == GroupVisibility.PRIVATE) {
            if (request.getPassword() == null || request.getPassword().isBlank()) {
                throw new BadRequestException("Un grupo privado necesita una contraseña de ingreso");
            }
            group.setJoinPassword(passwordEncoder.encode(request.getPassword()));
        }

        group.getMembers().add(user);

        PrivateGroup saved = groupRepository.save(group);
        rememberVisit(user, saved);
        return mapToResponse(saved, user);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<GroupResponseDTO> getAllGroups(Pageable pageable, String username, String search) {
        User user = findUser(username);
        Page<PrivateGroup> groupPage = groupRepository.findVisibleFor(
                user.getId(), GroupVisibility.PUBLIC, toNamePattern(search), pageable);
        List<GroupResponseDTO> content = groupPage.getContent().stream()
                .map(group -> mapToResponse(group, user))
                .collect(Collectors.toList());
        return PageResponseDTO.of(content, groupPage.getNumber(), groupPage.getSize(), groupPage.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public GroupResponseDTO getGroupById(Long groupId, String username) {
        User user = findUser(username);
        PrivateGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Grupo no encontrado"));
        if (group.isPrivate() && !isMember(group, user)) {
            throw new ResourceNotFoundException("Grupo no encontrado");
        }
        return mapToResponse(group, user);
    }

    @Override
    @Transactional(readOnly = true)
    public GroupResponseDTO getGroupByCode(String code, String username) {
        User user = findUser(username);
        PrivateGroup group = groupRepository.findByCodeIgnoreCase(code == null ? "" : code.trim())
                .orElseThrow(() -> new ResourceNotFoundException("No existe un grupo con ese código"));
        return mapToResponse(group, user);
    }

    @Override
    @Transactional
    public GroupResponseDTO joinGroup(Long groupId, String username, String password, String inviteToken) {
        User user = findUser(username);

        PrivateGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Grupo no encontrado"));

        if (groupRepository.existsByIdAndMembersId(groupId, user.getId())) {
            throw new DuplicateResourceException("Ya eres miembro de este grupo");
        }

        if (group.isPrivate() && !hasValidInvite(group, inviteToken)) {
            boolean valid = password != null && group.getJoinPassword() != null
                    && passwordEncoder.matches(password, group.getJoinPassword());
            if (!valid) {
                throw new ForbiddenException(inviteToken != null
                        ? "La invitación ya no es válida. Pide un enlace nuevo o la contraseña del grupo"
                        : "La contraseña del grupo no es correcta");
            }
        }

        group.getMembers().add(user);
        PrivateGroup saved = groupRepository.save(group);
        rememberVisit(user, saved);
        eventPublisher.publishEvent(new OnGroupActivityEvent(
                this, groupId, user.getId(), NotificationType.GROUP_MEMBER, user.getUsername()));
        return mapToResponse(saved, user);
    }

    @Override
    @Transactional
    public GroupInviteResponseDTO createInvite(Long groupId, String username, boolean regenerate) {
        PrivateGroup group = findOwnedGroup(groupId, findUser(username));
        boolean expired = group.getInviteExpiresAt() == null || group.getInviteExpiresAt().isBefore(LocalDateTime.now());
        if (regenerate || group.getInviteToken() == null || expired) {
            group.setInviteToken(codeGenerator.nextInviteToken());
            group.setInviteExpiresAt(LocalDateTime.now().plusDays(INVITE_DAYS));
            groupRepository.save(group);
        }
        return new GroupInviteResponseDTO(group.getCode(), group.getInviteToken(), group.getInviteExpiresAt());
    }

    @Override
    @Transactional
    public void changePassword(Long groupId, String newPassword, String username) {
        PrivateGroup group = findOwnedGroup(groupId, findUser(username));
        if (!group.isPrivate()) {
            throw new InvalidOperationException("Solo los grupos privados tienen contraseña");
        }
        group.setJoinPassword(passwordEncoder.encode(newPassword));
        groupRepository.save(group);
    }

    private PrivateGroup findOwnedGroup(Long groupId, User user) {
        PrivateGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Grupo no encontrado"));
        if (group.getCreator() == null || !group.getCreator().getId().equals(user.getId())) {
            throw new ForbiddenException("Solo el creador del grupo puede hacer esto");
        }
        return group;
    }

    private static boolean hasValidInvite(PrivateGroup group, String inviteToken) {
        return inviteToken != null && !inviteToken.isBlank()
                && inviteToken.equals(group.getInviteToken())
                && group.getInviteExpiresAt() != null
                && group.getInviteExpiresAt().isAfter(LocalDateTime.now());
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupResponseDTO> getMyGroups(String username) {
        User user = findUser(username);
        Map<Long, GroupVisit> visits = visitRepository.findByUserId(user.getId()).stream()
                .collect(Collectors.toMap(visit -> visit.getGroup().getId(), visit -> visit, (a, b) -> a));

        return groupRepository.findByMembersId(user.getId()).stream()
                .map(group -> {
                    GroupResponseDTO dto = mapToResponse(group, user);
                    GroupVisit visit = visits.get(group.getId());
                    LocalDateTime seenAt = visit != null ? visit.getLastSeenAt() : null;
                    if (visit != null) {
                        LocalDateTime chatSeenAt = visit.getChatSeenAt() != null ? visit.getChatSeenAt() : seenAt;
                        dto.setUnreadMessages(messageRepository
                                .countByGroupIdAndCreatedAtAfterAndAuthorIdNot(group.getId(), chatSeenAt, user.getId()));
                    }
                    if (seenAt != null) {
                        dto.setUnreadDocuments(documentRepository
                                .countByGroupIdAndUploadedAtAfterAndAuthorIdNot(group.getId(), seenAt, user.getId()));
                        dto.setUnreadSessions(sessionRepository
                                .countByPrivateGroupIdAndCreatedAtAfterAndCreatedByIdNot(group.getId(), seenAt, user.getId()));
                        dto.setUnreadCount(dto.getUnreadDocuments() + dto.getUnreadSessions());
                    }
                    return dto;
                })
                .sorted(Comparator
                        .comparing(GroupResponseDTO::getLastActivityAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(GroupResponseDTO::getId, Comparator.reverseOrder()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void markAsSeen(Long groupId, String username) {
        User user = findUser(username);
        PrivateGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Grupo no encontrado"));
        if (!groupRepository.existsByIdAndMembersId(groupId, user.getId())) {
            throw new ForbiddenException("Solo los miembros pueden marcar el grupo como visto");
        }
        rememberVisit(user, group);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupMemberResponseDTO> getMembers(Long groupId, String username) {
        User user = findUser(username);

        PrivateGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Grupo no encontrado"));

        if (!groupRepository.existsByIdAndMembersId(groupId, user.getId())) {
            throw new ForbiddenException("Solo los miembros del grupo pueden ver a sus integrantes");
        }

        Long creatorId = group.getCreator() != null ? group.getCreator().getId() : null;
        return group.getMembers().stream()
                .sorted(Comparator
                        .comparing((User member) -> !member.getId().equals(creatorId))
                        .thenComparing(member -> member.getUsername().toLowerCase()))
                .map(member -> mapToMember(member, creatorId))
                .collect(Collectors.toList());
    }

    private GroupMemberResponseDTO mapToMember(User member, Long creatorId) {
        GroupMemberResponseDTO dto = new GroupMemberResponseDTO();
        dto.setId(member.getId());
        dto.setUsername(member.getUsername());
        dto.setAvatarUrl(UserResponseDTO.avatarUrlFor(member.getId(), member.getAvatarVersion()));
        dto.setGroupRole(member.getId().equals(creatorId) ? "CREATOR" : "MEMBER");
        dto.setAdvisor(member.isAdvisor());
        return dto;
    }

    private void rememberVisit(User user, PrivateGroup group) {
        GroupVisit visit = visitRepository.findByUserIdAndGroupId(user.getId(), group.getId())
                .orElseGet(() -> new GroupVisit(user, group, LocalDateTime.now()));
        visit.setLastSeenAt(LocalDateTime.now());
        visitRepository.save(visit);
    }

    private static String toNamePattern(String search) {
        if (search == null || search.isBlank()) {
            return "%";
        }
        String sanitized = search.trim().toLowerCase()
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + sanitized + "%";
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    private boolean isMember(PrivateGroup group, User user) {
        return group.getMembers().stream().anyMatch(member -> member.getId().equals(user.getId()));
    }

    private GroupResponseDTO mapToResponse(PrivateGroup group, User viewer) {
        GroupResponseDTO response = new GroupResponseDTO();
        response.setId(group.getId());
        response.setName(group.getName());
        response.setDescription(group.getDescription());
        String creator = group.getCreator() != null
                ? group.getCreator().getUsername()
                : (group.getMembers().isEmpty() ? "Sin miembros" : group.getMembers().iterator().next().getUsername());
        response.setCreatorUsername(creator);
        response.setCode(group.getCode());
        response.setVisibility(group.isPrivate() ? GroupVisibility.PRIVATE : GroupVisibility.PUBLIC);
        response.setMember(isMember(group, viewer));
        response.setMemberCount(group.getMembers().size());
        response.setLastActivityAt(group.getLastActivityAt());
        return response;
    }
}

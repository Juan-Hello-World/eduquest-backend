package com.eduquest.api.service.impl;

import com.eduquest.api.dto.request.GroupRequestDTO;
import com.eduquest.api.dto.request.GroupUpdateRequestDTO;
import com.eduquest.api.dto.response.GroupInviteResponseDTO;
import com.eduquest.api.dto.response.GroupMemberResponseDTO;
import com.eduquest.api.dto.response.GroupResponseDTO;
import com.eduquest.api.dto.response.PageResponseDTO;
import com.eduquest.api.dto.response.UserResponseDTO;
import com.eduquest.api.entity.Availability;
import com.eduquest.api.entity.GroupVisibility;
import com.eduquest.api.entity.GroupVisit;
import com.eduquest.api.entity.NotificationType;
import com.eduquest.api.entity.PrivateGroup;
import com.eduquest.api.entity.RoleName;
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
import com.eduquest.api.service.DocumentService;
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
import java.util.Locale;
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
    private final DocumentService documentService;
    private final GroupChatCleaner chatCleaner;

    public GroupServiceImpl(PrivateGroupRepository groupRepository,
                            UserRepository userRepository,
                            PasswordEncoder passwordEncoder,
                            GroupCodeGenerator codeGenerator,
                            GroupVisitRepository visitRepository,
                            DocumentRepository documentRepository,
                            StudySessionRepository sessionRepository,
                            GroupMessageRepository messageRepository,
                            ApplicationEventPublisher eventPublisher,
                            DocumentService documentService,
                            GroupChatCleaner chatCleaner) {
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.codeGenerator = codeGenerator;
        this.visitRepository = visitRepository;
        this.documentRepository = documentRepository;
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.eventPublisher = eventPublisher;
        this.documentService = documentService;
        this.chatCleaner = chatCleaner;
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
        GroupResponseDTO response = mapToResponse(group, user);
        visitRepository.findByUserIdAndGroupId(user.getId(), groupId)
                .ifPresent(visit -> response.setMuted(visit.isMuted()));
        return response;
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

    @Override
    @Transactional
    public GroupResponseDTO updateGroup(Long groupId, GroupUpdateRequestDTO request, String username) {
        User user = findUser(username);
        PrivateGroup group = findOwnedGroup(groupId, user);
        group.setName(request.getName().trim());
        String description = request.getDescription();
        group.setDescription(description == null || description.isBlank() ? null : description.trim());
        return mapToResponse(groupRepository.save(group), user);
    }

    @Override
    @Transactional
    public void deleteGroup(Long groupId, String username) {
        User user = findUser(username);
        PrivateGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Grupo no encontrado"));
        boolean isCreator = group.getCreator() != null && group.getCreator().getId().equals(user.getId());
        if (!isCreator && !user.hasRole(RoleName.ROLE_ADMIN)) {
            throw new ForbiddenException("Solo el creador del grupo puede eliminarlo");
        }
        documentRepository.findAllByGroupId(groupId)
                .forEach(document -> documentService.deleteDocument(document.getId()));
        sessionRepository.deleteAll(sessionRepository.findByPrivateGroupId(groupId));
        chatCleaner.beforeMessages(groupId);
        messageRepository.clearRepliesByGroupId(groupId);
        messageRepository.deleteByGroupId(groupId);
        chatCleaner.afterMessages(groupId);
        visitRepository.deleteByGroupId(groupId);
        group.getModerators().clear();
        group.getMembers().clear();
        groupRepository.delete(group);
    }

    @Override
    @Transactional
    public void leaveGroup(Long groupId, String username) {
        User user = findUser(username);
        PrivateGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Grupo no encontrado"));
        if (!isMember(group, user)) {
            throw new InvalidOperationException("No eres miembro de este grupo");
        }
        if (group.getCreator() != null && group.getCreator().getId().equals(user.getId())) {
            throw new InvalidOperationException("Eres el creador del grupo: si ya no lo necesitas, elimínalo");
        }
        group.getMembers().removeIf(member -> member.getId().equals(user.getId()));
        group.getModerators().removeIf(moderator -> moderator.getId().equals(user.getId()));
        visitRepository.deleteByGroupIdAndUserId(groupId, user.getId());
        groupRepository.save(group);
    }

    @Override
    @Transactional
    public void removeMember(Long groupId, Long memberId, String username) {
        User user = findUser(username);
        PrivateGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Grupo no encontrado"));
        if (!group.canManage(user)) {
            throw new ForbiddenException("Solo el creador o un moderador del grupo puede hacer esto");
        }
        if (user.getId().equals(memberId)) {
            throw new InvalidOperationException("No puedes quitarte a ti mismo del grupo");
        }
        User target = group.getMembers().stream()
                .filter(member -> member.getId().equals(memberId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Esa persona no es miembro del grupo"));
        if (!group.isCreator(user) && (group.isCreator(target) || group.isModerator(target))) {
            throw new ForbiddenException("Un moderador no puede quitar al creador ni a otros moderadores");
        }
        group.getMembers().remove(target);
        group.getModerators().removeIf(moderator -> moderator.getId().equals(memberId));
        visitRepository.deleteByGroupIdAndUserId(groupId, memberId);
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
                        dto.setMuted(visit.isMuted());
                        LocalDateTime chatSeenAt = visit.getChatSeenAt() != null ? visit.getChatSeenAt() : seenAt;
                        dto.setUnreadMessages(messageRepository
                                .countByGroupIdAndCreatedAtAfterAndAuthorIdNot(group.getId(), chatSeenAt, user.getId()));
                        if (dto.getUnreadMessages() > 0) {
                            dto.setUnreadMentions(messageRepository.countMentionsSince(group.getId(), chatSeenAt, user.getId(),
                                    "%@" + GroupCommunityServiceImpl.likeEscape(user.getUsername().toLowerCase(Locale.ROOT)) + "%"));
                        }
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
                .map(member -> mapToMember(group, member, creatorId))
                .collect(Collectors.toList());
    }

    private GroupMemberResponseDTO mapToMember(PrivateGroup group, User member, Long creatorId) {
        GroupMemberResponseDTO dto = new GroupMemberResponseDTO();
        dto.setId(member.getId());
        dto.setUsername(member.getUsername());
        dto.setAvatarUrl(UserResponseDTO.avatarUrlFor(member.getId(), member.getAvatarVersion()));
        dto.setGroupRole(member.getId().equals(creatorId) ? "CREATOR"
                : group.isModerator(member) ? "MODERATOR" : "MEMBER");
        dto.setAdvisor(member.isAdvisor());
        if (member.getAvailability() != null && member.getAvailability() != Availability.INVISIBLE) {
            dto.setAvailability(member.getAvailability().name());
        }
        dto.setStatusText(member.getStatusText());
        dto.setStatusEmoji(member.getStatusEmoji());
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
        response.setCreatorUsername(group.getCreator() != null ? group.getCreator().getUsername() : null);
        response.setCode(group.getCode());
        response.setVisibility(group.isPrivate() ? GroupVisibility.PRIVATE : GroupVisibility.PUBLIC);
        response.setMember(isMember(group, viewer));
        response.setMemberCount(group.getMembers().size());
        response.setLastActivityAt(group.getLastActivityAt());
        response.setCanManage(group.canManage(viewer));
        return response;
    }
}

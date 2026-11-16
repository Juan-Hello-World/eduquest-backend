package com.eduquest.api.service.impl;

import com.eduquest.api.dto.request.GroupRequestDTO;
import com.eduquest.api.dto.response.GroupMemberResponseDTO;
import com.eduquest.api.dto.response.GroupResponseDTO;
import com.eduquest.api.dto.response.PageResponseDTO;
import com.eduquest.api.entity.GroupVisibility;
import com.eduquest.api.entity.GroupVisit;
import com.eduquest.api.entity.PrivateGroup;
import com.eduquest.api.entity.User;
import com.eduquest.api.exception.BadRequestException;
import com.eduquest.api.exception.ForbiddenException;
import com.eduquest.api.exception.InvalidOperationException;
import com.eduquest.api.exception.ResourceNotFoundException;
import com.eduquest.api.repository.DocumentRepository;
import com.eduquest.api.repository.GroupVisitRepository;
import com.eduquest.api.repository.PrivateGroupRepository;
import com.eduquest.api.repository.StudySessionRepository;
import com.eduquest.api.repository.UserRepository;
import com.eduquest.api.service.GroupCodeGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GroupServiceImplTest {

    @Mock PrivateGroupRepository groupRepository;
    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock GroupCodeGenerator codeGenerator;
    @Mock GroupVisitRepository visitRepository;
    @Mock DocumentRepository documentRepository;
    @Mock StudySessionRepository sessionRepository;
    @Mock com.eduquest.api.repository.GroupMessageRepository messageRepository;
    @Mock org.springframework.context.ApplicationEventPublisher eventPublisher;
    @Mock com.eduquest.api.service.DocumentService documentService;
    @Mock GroupChatCleaner chatCleaner;
    @Mock com.eduquest.api.service.FileStorageService fileStorageService;

    @InjectMocks GroupServiceImpl groupService;

    private User alice;

    @BeforeEach
    void setUp() {
        alice = new User();
        alice.setId(1L);
        alice.setUsername("alice");
        alice.setEmail("alice@utec.edu.pe");
        when(codeGenerator.nextCode()).thenReturn("K7P2QX");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
    }

    @Test
    void createGroup_withExistingUser_createsAndAddsMember() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(groupRepository.save(any(PrivateGroup.class))).thenAnswer(invocation -> {
            PrivateGroup saved = invocation.getArgument(0);
            saved.setId(5L);
            return saved;
        });

        GroupRequestDTO request = new GroupRequestDTO();
        request.setName("Grupo de Calculo");
        request.setDescription("Calculo I");

        GroupResponseDTO response = groupService.createGroup(request, "alice");

        assertThat(response.getId()).isEqualTo(5L);
        assertThat(response.getName()).isEqualTo("Grupo de Calculo");
        assertThat(response.getCreatorUsername()).isEqualTo("alice");
        assertThat(response.getCode()).isEqualTo("K7P2QX");
        assertThat(response.getVisibility()).isEqualTo(GroupVisibility.PUBLIC);
        assertThat(response.isMember()).isTrue();
        assertThat(response.getMemberCount()).isEqualTo(1);
    }

    @Test
    void createGroup_private_hashesPassword() {
        when(passwordEncoder.encode("secreto")).thenReturn("hash");
        when(groupRepository.save(any(PrivateGroup.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GroupRequestDTO request = new GroupRequestDTO();
        request.setName("Grupo cerrado");
        request.setVisibility(GroupVisibility.PRIVATE);
        request.setPassword("secreto");

        GroupResponseDTO response = groupService.createGroup(request, "alice");

        ArgumentCaptor<PrivateGroup> captor = ArgumentCaptor.forClass(PrivateGroup.class);
        verify(groupRepository).save(captor.capture());
        assertThat(captor.getValue().getJoinPassword()).isEqualTo("hash");
        assertThat(response.getVisibility()).isEqualTo(GroupVisibility.PRIVATE);
    }

    @Test
    void createGroup_privateWithoutPassword_throwsBadRequest() {
        GroupRequestDTO request = new GroupRequestDTO();
        request.setName("Grupo cerrado");
        request.setVisibility(GroupVisibility.PRIVATE);
        request.setPassword("  ");

        assertThatThrownBy(() -> groupService.createGroup(request, "alice"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void createGroup_withUnknownUser_throwsNotFound() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        GroupRequestDTO request = new GroupRequestDTO();
        request.setName("G");
        request.setDescription("D");

        assertThatThrownBy(() -> groupService.createGroup(request, "ghost"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getAllGroups_returnsPageAndFallsBackCreatorLabel() {
        PrivateGroup withMember = new PrivateGroup();
        withMember.setId(1L);
        withMember.setName("Con miembro");
        withMember.setCreator(alice);
        withMember.getMembers().add(alice);

        PrivateGroup empty = new PrivateGroup();
        empty.setId(2L);
        empty.setName("Vacio");

        Pageable pageable = PageRequest.of(0, 10);
        when(groupRepository.findVisibleFor(1L, GroupVisibility.PUBLIC, "%", pageable))
                .thenReturn(new PageImpl<>(List.of(withMember, empty), pageable, 2));

        PageResponseDTO<GroupResponseDTO> page = groupService.getAllGroups(pageable, "alice", null);

        assertThat(page.content()).hasSize(2);
        assertThat(page.content().get(0).getCreatorUsername()).isEqualTo("alice");
        assertThat(page.content().get(1).getCreatorUsername()).isNull();
        assertThat(page.content().get(0).isMember()).isTrue();
        assertThat(page.content().get(1).isMember()).isFalse();
    }

    @Test
    void getGroupById_whenExists_returnsDto() {
        PrivateGroup group = new PrivateGroup();
        group.setId(1L);
        group.setName("Grupo de Calculo");
        group.setDescription("Calculo I");
        group.setCreator(alice);

        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));

        GroupResponseDTO response = groupService.getGroupById(1L, "alice");

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Grupo de Calculo");
        assertThat(response.getCreatorUsername()).isEqualTo("alice");
    }

    @Test
    void getGroupById_whenMissing_throwsNotFound() {
        when(groupRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> groupService.getGroupById(99L, "alice"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getGroupById_privateForOutsider_isHidden() {
        PrivateGroup group = privateGroup();
        when(groupRepository.findById(3L)).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> groupService.getGroupById(3L, "alice"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getGroupByCode_findsPrivateGroupIgnoringCase() {
        PrivateGroup group = privateGroup();
        when(groupRepository.findByCodeIgnoreCase("ab12cd")).thenReturn(Optional.of(group));

        GroupResponseDTO response = groupService.getGroupByCode(" ab12cd ", "alice");

        assertThat(response.getCode()).isEqualTo("AB12CD");
        assertThat(response.getVisibility()).isEqualTo(GroupVisibility.PRIVATE);
        assertThat(response.isMember()).isFalse();
    }

    @Test
    void getGroupByCode_whenMissing_throwsNotFound() {
        when(groupRepository.findByCodeIgnoreCase(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> groupService.getGroupByCode(null, "alice"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void joinGroup_privateWithCorrectPassword_addsMember() {
        PrivateGroup group = privateGroup();
        when(groupRepository.findById(3L)).thenReturn(Optional.of(group));
        when(passwordEncoder.matches("secreto", "hash")).thenReturn(true);
        when(groupRepository.save(any(PrivateGroup.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GroupResponseDTO response = groupService.joinGroup(3L, "alice", "secreto");

        assertThat(response.isMember()).isTrue();
        assertThat(response.getMemberCount()).isEqualTo(2);
    }

    @Test
    void joinGroup_privateWithWrongPassword_throwsForbidden() {
        PrivateGroup group = privateGroup();
        when(groupRepository.findById(3L)).thenReturn(Optional.of(group));
        when(passwordEncoder.matches("otra", "hash")).thenReturn(false);

        assertThatThrownBy(() -> groupService.joinGroup(3L, "alice", "otra"))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> groupService.joinGroup(3L, "alice", null))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void joinGroup_asNewMember_addsUserAndReturnsDto() {
        PrivateGroup group = new PrivateGroup();
        group.setId(1L);
        group.setName("Grupo de Calculo");
        group.setCreator(alice);

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(groupRepository.existsByIdAndMembersId(1L, 1L)).thenReturn(false);
        when(groupRepository.save(any(PrivateGroup.class))).thenReturn(group);

        GroupResponseDTO response = groupService.joinGroup(1L, "alice", null);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getCreatorUsername()).isEqualTo("alice");
    }

    @Test
    void joinGroup_whenAlreadyMember_throwsDuplicateResource() {
        PrivateGroup group = new PrivateGroup();
        group.setId(1L);

        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(groupRepository.existsByIdAndMembersId(1L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> groupService.joinGroup(1L, "alice", null))
                .isInstanceOf(com.eduquest.api.exception.DuplicateResourceException.class);
    }

    @Test
    void getMembers_asMember_returnsCreatorFirstWithAvatar() {
        User bob = new User();
        bob.setId(2L);
        bob.setUsername("bob");
        bob.setAvatarVersion(99L);

        PrivateGroup group = new PrivateGroup();
        group.setId(1L);
        group.setCreator(alice);
        group.getMembers().add(bob);
        group.getMembers().add(alice);

        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(bob));
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(groupRepository.existsByIdAndMembersId(1L, 2L)).thenReturn(true);

        List<GroupMemberResponseDTO> members = groupService.getMembers(1L, "bob");

        assertThat(members).extracting(GroupMemberResponseDTO::getUsername).containsExactly("alice", "bob");
        assertThat(members.get(0).getGroupRole()).isEqualTo("CREATOR");
        assertThat(members.get(1).getGroupRole()).isEqualTo("MEMBER");
        assertThat(members.get(1).getAvatarUrl()).isEqualTo("/api/v1/users/2/avatar?v=99");
    }

    @Test
    void getMembers_notMember_throwsForbidden() {
        PrivateGroup group = new PrivateGroup();
        group.setId(1L);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(groupRepository.existsByIdAndMembersId(1L, 1L)).thenReturn(false);

        assertThatThrownBy(() -> groupService.getMembers(1L, "alice"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void getMembers_unknownGroup_throwsNotFound() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(groupRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> groupService.getMembers(9L, "alice"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private PrivateGroup privateGroup() {
        User bob = new User();
        bob.setId(2L);
        bob.setUsername("bob");

        PrivateGroup group = new PrivateGroup();
        group.setId(3L);
        group.setName("Grupo cerrado");
        group.setCode("AB12CD");
        group.setVisibility(GroupVisibility.PRIVATE);
        group.setJoinPassword("hash");
        group.setCreator(bob);
        group.getMembers().add(bob);
        return group;
    }

    @Test
    void getAllGroups_withSearch_buildsEscapedPattern() {
        Pageable pageable = PageRequest.of(0, 10);
        when(groupRepository.findVisibleFor(1L, GroupVisibility.PUBLIC, "%calc\\_100%", pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        PageResponseDTO<GroupResponseDTO> page = groupService.getAllGroups(pageable, "alice", "  Calc_100 ");

        assertThat(page.content()).isEmpty();
        verify(groupRepository).findVisibleFor(1L, GroupVisibility.PUBLIC, "%calc\\_100%", pageable);
    }

    @Test
    void getMyGroups_countsUnreadAndSortsByActivity() {
        PrivateGroup older = new PrivateGroup();
        older.setId(1L);
        older.setName("Antiguo");
        older.setCreator(alice);
        older.getMembers().add(alice);
        older.setLastActivityAt(java.time.LocalDateTime.now().minusDays(2));

        PrivateGroup recent = new PrivateGroup();
        recent.setId(2L);
        recent.setName("Reciente");
        recent.setCreator(alice);
        recent.getMembers().add(alice);
        recent.setLastActivityAt(java.time.LocalDateTime.now());

        PrivateGroup legacy = new PrivateGroup();
        legacy.setId(3L);
        legacy.setName("Sin actividad");
        legacy.getMembers().add(alice);

        java.time.LocalDateTime seen = java.time.LocalDateTime.now().minusDays(1);
        when(groupRepository.findByMembersId(1L)).thenReturn(List.of(older, recent, legacy));
        when(visitRepository.findByUserId(1L)).thenReturn(List.of(
                new GroupVisit(alice, older, seen), new GroupVisit(alice, recent, seen)));
        when(documentRepository.countByGroupIdAndUploadedAtAfterAndAuthorIdNot(2L, seen, 1L)).thenReturn(2L);
        when(sessionRepository.countByPrivateGroupIdAndCreatedAtAfterAndCreatedByIdNot(2L, seen, 1L)).thenReturn(1L);
        when(messageRepository.countByGroupIdAndCreatedAtAfterAndAuthorIdNot(2L, seen, 1L)).thenReturn(4L);

        List<GroupResponseDTO> mine = groupService.getMyGroups("alice");

        assertThat(mine).extracting(GroupResponseDTO::getName).containsExactly("Reciente", "Antiguo", "Sin actividad");
        assertThat(mine.get(0).getUnreadCount()).isEqualTo(3);
        assertThat(mine.get(0).getUnreadDocuments()).isEqualTo(2);
        assertThat(mine.get(0).getUnreadMessages()).isEqualTo(4);
        assertThat(mine.get(1).getUnreadCount()).isZero();
        assertThat(mine.get(2).getUnreadCount()).isZero();
    }

    @Test
    void markAsSeen_asMember_updatesVisit() {
        PrivateGroup group = privateGroup();
        GroupVisit visit = new GroupVisit(alice, group, java.time.LocalDateTime.now().minusDays(3));
        when(groupRepository.findById(3L)).thenReturn(Optional.of(group));
        when(groupRepository.existsByIdAndMembersId(3L, 1L)).thenReturn(true);
        when(visitRepository.findByUserIdAndGroupId(1L, 3L)).thenReturn(Optional.of(visit));

        groupService.markAsSeen(3L, "alice");

        assertThat(visit.getLastSeenAt()).isAfter(java.time.LocalDateTime.now().minusMinutes(1));
        verify(visitRepository).save(visit);
    }

    @Test
    void markAsSeen_asOutsider_throwsForbidden() {
        when(groupRepository.findById(3L)).thenReturn(Optional.of(privateGroup()));
        when(groupRepository.existsByIdAndMembersId(3L, 1L)).thenReturn(false);

        assertThatThrownBy(() -> groupService.markAsSeen(3L, "alice"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void joinGroup_createsVisitForNewMember() {
        PrivateGroup group = new PrivateGroup();
        group.setId(1L);
        group.setName("Publico");
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(groupRepository.existsByIdAndMembersId(1L, 1L)).thenReturn(false);
        when(groupRepository.save(any(PrivateGroup.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(visitRepository.findByUserIdAndGroupId(1L, 1L)).thenReturn(Optional.empty());

        groupService.joinGroup(1L, "alice", null);

        verify(visitRepository).save(any(GroupVisit.class));
    }

    private PrivateGroup ownedPrivateGroup() {
        PrivateGroup group = privateGroup();
        group.setCreator(alice);
        return group;
    }

    @Test
    void createInvite_byOwner_createsTokenValidForSevenDays() {
        PrivateGroup group = ownedPrivateGroup();
        when(groupRepository.findById(3L)).thenReturn(Optional.of(group));
        when(codeGenerator.nextInviteToken()).thenReturn("tok-1", "tok-2");

        com.eduquest.api.dto.response.GroupInviteResponseDTO first = groupService.createInvite(3L, "alice", false);
        com.eduquest.api.dto.response.GroupInviteResponseDTO reused = groupService.createInvite(3L, "alice", false);
        com.eduquest.api.dto.response.GroupInviteResponseDTO renewed = groupService.createInvite(3L, "alice", true);

        assertThat(first.token()).isEqualTo("tok-1");
        assertThat(first.code()).isEqualTo("AB12CD");
        assertThat(first.expiresAt()).isAfter(java.time.LocalDateTime.now().plusDays(6));
        assertThat(reused.token()).isEqualTo("tok-1");
        assertThat(renewed.token()).isEqualTo("tok-2");
    }

    @Test
    void createInvite_byNonOwner_throwsForbidden() {
        when(groupRepository.findById(3L)).thenReturn(Optional.of(privateGroup()));

        assertThatThrownBy(() -> groupService.createInvite(3L, "alice", false))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void joinGroup_privateWithValidInvite_skipsPassword() {
        PrivateGroup group = privateGroup();
        group.setInviteToken("tok");
        group.setInviteExpiresAt(java.time.LocalDateTime.now().plusDays(1));
        when(groupRepository.findById(3L)).thenReturn(Optional.of(group));
        when(groupRepository.save(any(PrivateGroup.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(groupService.joinGroup(3L, "alice", null, "tok").isMember()).isTrue();
    }

    @Test
    void joinGroup_privateWithExpiredInvite_throwsForbidden() {
        PrivateGroup group = privateGroup();
        group.setInviteToken("tok");
        group.setInviteExpiresAt(java.time.LocalDateTime.now().minusMinutes(1));
        when(groupRepository.findById(3L)).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> groupService.joinGroup(3L, "alice", null, "tok"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("invitación");
        assertThatThrownBy(() -> groupService.joinGroup(3L, "alice", null, "otro"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void changePassword_byOwner_encodesNewPassword() {
        PrivateGroup group = ownedPrivateGroup();
        when(groupRepository.findById(3L)).thenReturn(Optional.of(group));
        when(passwordEncoder.encode("nueva123")).thenReturn("hash-nuevo");

        groupService.changePassword(3L, "nueva123", "alice");

        assertThat(group.getJoinPassword()).isEqualTo("hash-nuevo");
    }

    @Test
    void changePassword_onPublicGroupOrMissing_fails() {
        PrivateGroup group = ownedPrivateGroup();
        group.setVisibility(GroupVisibility.PUBLIC);
        when(groupRepository.findById(3L)).thenReturn(Optional.of(group));
        when(groupRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> groupService.changePassword(3L, "nueva123", "alice"))
                .isInstanceOf(com.eduquest.api.exception.InvalidOperationException.class);
        assertThatThrownBy(() -> groupService.changePassword(9L, "nueva123", "alice"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void joinGroup_publishesMemberJoinedEvent() {
        PrivateGroup group = privateGroup();
        when(groupRepository.findById(3L)).thenReturn(Optional.of(group));
        when(passwordEncoder.matches("secreto", "hash")).thenReturn(true);
        when(groupRepository.save(any(PrivateGroup.class))).thenAnswer(invocation -> invocation.getArgument(0));

        groupService.joinGroup(3L, "alice", "secreto");

        ArgumentCaptor<com.eduquest.api.event.OnGroupActivityEvent> captor =
                ArgumentCaptor.forClass(com.eduquest.api.event.OnGroupActivityEvent.class);
        org.mockito.Mockito.verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(com.eduquest.api.entity.NotificationType.GROUP_MEMBER);
        assertThat(captor.getValue().getActorId()).isEqualTo(1L);
    }

    private PrivateGroup ownedGroup() {
        PrivateGroup group = new PrivateGroup();
        group.setId(9L);
        group.setName("Mío");
        group.setCreator(alice);
        group.getMembers().add(alice);
        when(groupRepository.findById(9L)).thenReturn(Optional.of(group));
        when(groupRepository.save(any(PrivateGroup.class))).thenAnswer(invocation -> invocation.getArgument(0));
        return group;
    }

    @Test
    void updateGroup_asCreator_changesNameAndDescription() {
        ownedGroup();
        com.eduquest.api.dto.request.GroupUpdateRequestDTO request = new com.eduquest.api.dto.request.GroupUpdateRequestDTO();
        request.setName("  Cálculo II ");
        request.setDescription("  ");

        GroupResponseDTO response = groupService.updateGroup(9L, request, "alice");

        assertThat(response.getName()).isEqualTo("Cálculo II");
        assertThat(response.getDescription()).isNull();
    }

    @Test
    void deleteGroup_asCreator_removesContentAndGroup() {
        PrivateGroup group = ownedGroup();
        com.eduquest.api.entity.Document document = new com.eduquest.api.entity.Document();
        document.setId(4L);
        when(documentRepository.findAllByGroupId(9L)).thenReturn(List.of(document));

        groupService.deleteGroup(9L, "alice");

        verify(documentService).deleteDocument(4L);
        verify(messageRepository).deleteByGroupId(9L);
        verify(visitRepository).deleteByGroupId(9L);
        verify(groupRepository).delete(group);
    }

    @Test
    void deleteGroup_byAdminOrStranger() {
        PrivateGroup group = privateGroup();
        when(groupRepository.findById(3L)).thenReturn(Optional.of(group));

        assertThatThrownBy(() -> groupService.deleteGroup(3L, "alice")).isInstanceOf(ForbiddenException.class);

        com.eduquest.api.entity.Role admin = new com.eduquest.api.entity.Role();
        admin.setName(com.eduquest.api.entity.RoleName.ROLE_ADMIN);
        alice.getRoles().add(admin);
        groupService.deleteGroup(3L, "alice");
        verify(groupRepository).delete(group);

        when(groupRepository.findById(50L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> groupService.deleteGroup(50L, "alice")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void leaveGroup_memberLeaves_creatorCannot() {
        PrivateGroup group = privateGroup();
        group.getMembers().add(alice);
        when(groupRepository.findById(3L)).thenReturn(Optional.of(group));

        groupService.leaveGroup(3L, "alice");

        assertThat(group.getMembers()).extracting(User::getId).doesNotContain(1L);
        verify(visitRepository).deleteByGroupIdAndUserId(3L, 1L);
        assertThatThrownBy(() -> groupService.leaveGroup(3L, "alice")).isInstanceOf(InvalidOperationException.class);

        ownedGroup();
        assertThatThrownBy(() -> groupService.leaveGroup(9L, "alice")).isInstanceOf(InvalidOperationException.class);

        when(groupRepository.findById(50L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> groupService.leaveGroup(50L, "alice")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void removeMember_asCreator() {
        PrivateGroup group = ownedGroup();
        User bob = new User();
        bob.setId(2L);
        group.getMembers().add(bob);

        groupService.removeMember(9L, 2L, "alice");

        assertThat(group.getMembers()).extracting(User::getId).containsExactly(1L);
        verify(visitRepository).deleteByGroupIdAndUserId(9L, 2L);
        assertThatThrownBy(() -> groupService.removeMember(9L, 2L, "alice")).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> groupService.removeMember(9L, 1L, "alice")).isInstanceOf(InvalidOperationException.class);
    }

    @Test
    void removeMember_moderatorCannotRemoveManagers() {
        PrivateGroup group = ownedGroup();
        User mod = new User();
        mod.setId(2L);
        mod.setUsername("mod");
        User other = new User();
        other.setId(3L);
        other.setUsername("other");
        User peer = new User();
        peer.setId(4L);
        peer.setUsername("peer");
        group.getMembers().add(mod);
        group.getMembers().add(other);
        group.getMembers().add(peer);
        group.getModerators().add(mod);
        group.getModerators().add(peer);
        when(userRepository.findByUsername("mod")).thenReturn(Optional.of(mod));
        when(userRepository.findByUsername("other")).thenReturn(Optional.of(other));

        groupService.removeMember(9L, 3L, "mod");

        assertThat(group.getMembers()).extracting(User::getId).doesNotContain(3L);
        assertThatThrownBy(() -> groupService.removeMember(9L, 1L, "mod")).isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> groupService.removeMember(9L, 4L, "mod")).isInstanceOf(ForbiddenException.class);
        groupService.removeMember(9L, 4L, "alice");
        assertThat(group.getModerators()).extracting(User::getId).containsExactly(2L);
        assertThatThrownBy(() -> groupService.removeMember(9L, 2L, "other")).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void getMembers_marksModerators() {
        PrivateGroup group = ownedGroup();
        User mod = new User();
        mod.setId(2L);
        mod.setUsername("mod");
        group.getMembers().add(mod);
        group.getModerators().add(mod);
        when(groupRepository.existsByIdAndMembersId(9L, 1L)).thenReturn(true);

        List<GroupMemberResponseDTO> members = groupService.getMembers(9L, "alice");

        assertThat(members).extracting(GroupMemberResponseDTO::getGroupRole).containsExactly("CREATOR", "MODERATOR");
    }

    @Test
    void deleteGroup_cleansChatExtras() {
        PrivateGroup group = ownedGroup();

        groupService.deleteGroup(9L, "alice");

        verify(chatCleaner).beforeMessages(9L);
        verify(chatCleaner).afterMessages(9L);
        assertThat(group.getModerators()).isEmpty();
    }

    private PrivateGroup managedGroup() {
        PrivateGroup group = new PrivateGroup();
        group.setId(30L);
        group.setName("Teoria");
        group.setCreator(alice);
        group.getMembers().add(alice);
        group.setIconUrl("/api/v1/files/11111111-1111-1111-1111-111111111111");
        when(groupRepository.findById(30L)).thenReturn(Optional.of(group));
        when(groupRepository.save(any(PrivateGroup.class))).thenAnswer(invocation -> invocation.getArgument(0));
        return group;
    }

    private static com.eduquest.api.dto.response.StoredFileDTO file(String contentType) {
        return new com.eduquest.api.dto.response.StoredFileDTO(new byte[] {1}, contentType, "archivo");
    }

    @Test
    void updateAppearance_savesImagesAndColorAndReplacesOldIcon() {
        PrivateGroup group = managedGroup();
        String icon = "/api/v1/files/22222222-2222-2222-2222-222222222222";
        String banner = "/api/v1/files/33333333-3333-3333-3333-333333333333";
        com.eduquest.api.dto.response.StoredFileDTO png = file("image/png");
        com.eduquest.api.dto.response.StoredFileDTO jpg = file("image/jpeg");
        when(fileStorageService.loadByUrl(icon)).thenReturn(Optional.of(png));
        when(fileStorageService.loadByUrl(banner)).thenReturn(Optional.of(jpg));
        com.eduquest.api.dto.request.GroupAppearanceRequestDTO request = new com.eduquest.api.dto.request.GroupAppearanceRequestDTO();
        request.setColor("#FFD25E");
        request.setIconUrl(" " + icon + " ");
        request.setBannerUrl(banner);

        GroupResponseDTO response = groupService.updateAppearance(30L, request, "alice");

        assertThat(response.getColor()).isEqualTo("#ffd25e");
        assertThat(response.getIconUrl()).isEqualTo(icon);
        assertThat(response.getBannerUrl()).isEqualTo(banner);
        verify(fileStorageService).deleteByUrl("/api/v1/files/11111111-1111-1111-1111-111111111111");
        assertThat(group.getBannerUrl()).isEqualTo(banner);
    }

    @Test
    void updateAppearance_clearsValuesWhenBlank() {
        PrivateGroup group = managedGroup();
        group.setColor("#3ee6a8");
        com.eduquest.api.dto.request.GroupAppearanceRequestDTO request = new com.eduquest.api.dto.request.GroupAppearanceRequestDTO();
        request.setColor(" ");
        request.setIconUrl("");

        GroupResponseDTO response = groupService.updateAppearance(30L, request, "alice");

        assertThat(response.getColor()).isNull();
        assertThat(response.getIconUrl()).isNull();
        assertThat(response.getBannerUrl()).isNull();
        verify(fileStorageService).deleteByUrl("/api/v1/files/11111111-1111-1111-1111-111111111111");
    }

    @Test
    void updateAppearance_rejectsFilesThatAreNotImages() {
        managedGroup();
        String pdf = "/api/v1/files/44444444-4444-4444-4444-444444444444";
        com.eduquest.api.dto.response.StoredFileDTO document = file("application/pdf");
        when(fileStorageService.loadByUrl(pdf)).thenReturn(Optional.of(document));
        com.eduquest.api.dto.request.GroupAppearanceRequestDTO request = new com.eduquest.api.dto.request.GroupAppearanceRequestDTO();
        request.setIconUrl(pdf);

        assertThatThrownBy(() -> groupService.updateAppearance(30L, request, "alice")).isInstanceOf(BadRequestException.class);

        com.eduquest.api.dto.request.GroupAppearanceRequestDTO missing = new com.eduquest.api.dto.request.GroupAppearanceRequestDTO();
        missing.setBannerUrl("/api/v1/files/55555555-5555-5555-5555-555555555555");
        when(fileStorageService.loadByUrl(missing.getBannerUrl())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> groupService.updateAppearance(30L, missing, "alice")).isInstanceOf(BadRequestException.class);
    }

    @Test
    void updateAppearance_forbiddenForMembersWhoCannotManage() {
        PrivateGroup group = managedGroup();
        User bob = new User();
        bob.setId(2L);
        bob.setUsername("bob");
        group.getMembers().add(bob);
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(bob));

        assertThatThrownBy(() -> groupService.updateAppearance(30L, new com.eduquest.api.dto.request.GroupAppearanceRequestDTO(), "bob"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void updateAppearance_missingGroup_throwsNotFound() {
        when(groupRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> groupService.updateAppearance(99L, new com.eduquest.api.dto.request.GroupAppearanceRequestDTO(), "alice"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createGroup_storesNormalizedColor() {
        when(groupRepository.save(any(PrivateGroup.class))).thenAnswer(invocation -> invocation.getArgument(0));
        GroupRequestDTO request = new GroupRequestDTO();
        request.setName("Con color");
        request.setColor("#FF6FB5");

        GroupResponseDTO response = groupService.createGroup(request, "alice");

        assertThat(response.getColor()).isEqualTo("#ff6fb5");
    }
}

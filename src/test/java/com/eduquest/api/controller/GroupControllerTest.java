package com.eduquest.api.controller;

import com.eduquest.api.dto.request.DocumentRequestDTO;
import com.eduquest.api.dto.request.GroupRequestDTO;
import com.eduquest.api.dto.request.JoinGroupRequestDTO;
import com.eduquest.api.dto.response.DocumentResponseDTO;
import com.eduquest.api.dto.response.GroupMemberResponseDTO;
import com.eduquest.api.dto.response.GroupResponseDTO;
import com.eduquest.api.dto.response.PageResponseDTO;
import com.eduquest.api.service.DocumentService;
import com.eduquest.api.service.GroupService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GroupControllerTest {

    @Mock GroupService groupService;
    @Mock DocumentService documentService;

    @InjectMocks GroupController groupController;

    private GroupResponseDTO response() {
        GroupResponseDTO dto = new GroupResponseDTO();
        dto.setId(1L);
        dto.setName("Grupo de Calculo");
        dto.setDescription("Estudio grupal");
        dto.setCreatorUsername("alice");
        return dto;
    }

    @Test
    void createGroup_returnsCreated() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("alice");
        when(groupService.createGroup(any(), anyString())).thenReturn(response());

        GroupRequestDTO request = new GroupRequestDTO();
        request.setName("Grupo de Calculo");
        request.setDescription("Estudio grupal");

        ResponseEntity<GroupResponseDTO> result = groupController.createGroup(request, authentication);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody().getCreatorUsername()).isEqualTo("alice");
    }

    @Test
    void getAllGroups_returnsPaginatedResponse() {
        PageResponseDTO<GroupResponseDTO> page =
                new PageResponseDTO<>(List.of(response()), 0, 10, 1, 1);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("alice");
        when(groupService.getAllGroups(any(), anyString(), any())).thenReturn(page);

        ResponseEntity<PageResponseDTO<GroupResponseDTO>> result = groupController.getAllGroups(0, 10, "calc", authentication);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().content()).hasSize(1);
    }

    @Test
    void getGroupById_returnsOk() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("alice");
        when(groupService.getGroupById(1L, "alice")).thenReturn(response());

        ResponseEntity<GroupResponseDTO> result = groupController.getGroupById(1L, authentication);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getName()).isEqualTo("Grupo de Calculo");
    }

    @Test
    void getGroupByCode_returnsOk() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("alice");
        when(groupService.getGroupByCode("AB12CD", "alice")).thenReturn(response());

        ResponseEntity<GroupResponseDTO> result = groupController.getGroupByCode("AB12CD", authentication);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getId()).isEqualTo(1L);
    }

    @Test
    void uploadDocumentToGroup_returnsCreated() {
        DocumentResponseDTO dto = new DocumentResponseDTO();
        dto.setId(1L);
        dto.setTitle("Tarea de grupo");
        dto.setFileUrl("https://cloud.example/tarea.pdf");
        dto.setUploadedAt(LocalDateTime.now());
        dto.setAuthorUsername("alice");
        dto.setGroupName("Grupo de Calculo");

        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("alice");
        when(documentService.uploadDocumentToGroup(anyLong(), any(), anyString())).thenReturn(dto);

        DocumentRequestDTO request = new DocumentRequestDTO();
        request.setTitle("Tarea de grupo");
        request.setFileUrl("https://cloud.example/tarea.pdf");

        ResponseEntity<DocumentResponseDTO> result = groupController.uploadDocumentToGroup(1L, request, authentication);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody().getGroupName()).isEqualTo("Grupo de Calculo");
    }
    @Test
    void joinGroup_returnsOk() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("alice");
        when(groupService.joinGroup(anyLong(), anyString(), isNull(), isNull())).thenReturn(response());

        ResponseEntity<GroupResponseDTO> result = groupController.joinGroup(1L, null, authentication);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getName()).isEqualTo("Grupo de Calculo");
    }

    @Test
    void joinGroup_withPassword_forwardsIt() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("alice");
        when(groupService.joinGroup(1L, "alice", "secreto", null)).thenReturn(response());

        JoinGroupRequestDTO request = new JoinGroupRequestDTO();
        request.setPassword("secreto");

        ResponseEntity<GroupResponseDTO> result = groupController.joinGroup(1L, request, authentication);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void getGroupDocuments_returnsPaginatedResponse() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("alice");
        PageResponseDTO<DocumentResponseDTO> page =
                new PageResponseDTO<DocumentResponseDTO>(List.of(), 0, 10, 0, 0);
        when(documentService.getDocumentsByGroup(anyLong(), anyString(), any())).thenReturn(page);

        ResponseEntity<PageResponseDTO<DocumentResponseDTO>> result =
                groupController.getGroupDocuments(1L, 0, 10, authentication);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void getGroupMembers_returnsOk() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("alice");
        GroupMemberResponseDTO member = new GroupMemberResponseDTO();
        member.setUsername("alice");
        member.setGroupRole("CREATOR");
        when(groupService.getMembers(1L, "alice")).thenReturn(List.of(member));

        ResponseEntity<List<GroupMemberResponseDTO>> result = groupController.getGroupMembers(1L, authentication);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).hasSize(1);
    }

    @Test
    void getMyGroups_returnsOk() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("alice");
        when(groupService.getMyGroups("alice")).thenReturn(List.of(response()));

        ResponseEntity<List<GroupResponseDTO>> result = groupController.getMyGroups(authentication);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).hasSize(1);
    }

    @Test
    void markAsSeen_returnsNoContent() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("alice");

        ResponseEntity<Void> result = groupController.markAsSeen(1L, authentication);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        org.mockito.Mockito.verify(groupService).markAsSeen(1L, "alice");
    }

    @Test
    void inviteAndPasswordEndpoints_delegateToService() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("alice");
        com.eduquest.api.dto.response.GroupInviteResponseDTO invite =
                new com.eduquest.api.dto.response.GroupInviteResponseDTO("AB12CD", "token", LocalDateTime.now().plusDays(7));
        when(groupService.createInvite(1L, "alice", true)).thenReturn(invite);
        com.eduquest.api.dto.request.GroupPasswordRequestDTO request = new com.eduquest.api.dto.request.GroupPasswordRequestDTO();
        request.setPassword("nueva123");

        assertThat(groupController.createInvite(1L, true, authentication).getBody().token()).isEqualTo("token");
        assertThat(groupController.changePassword(1L, request, authentication).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        org.mockito.Mockito.verify(groupService).changePassword(1L, "nueva123", "alice");
    }

    @Test
    void joinGroup_withInviteToken_forwardsIt() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("alice");
        when(groupService.joinGroup(1L, "alice", null, "token")).thenReturn(response());
        JoinGroupRequestDTO request = new JoinGroupRequestDTO();
        request.setInviteToken("token");

        assertThat(groupController.joinGroup(1L, request, authentication).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void manageGroupEndpoints_delegateToService() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("alice");
        com.eduquest.api.dto.request.GroupUpdateRequestDTO request = new com.eduquest.api.dto.request.GroupUpdateRequestDTO();
        when(groupService.updateGroup(1L, request, "alice")).thenReturn(response());

        assertThat(groupController.updateGroup(1L, request, authentication).getBody().getName()).isEqualTo("Grupo de Calculo");
        assertThat(groupController.deleteGroup(1L, authentication).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(groupController.leaveGroup(1L, authentication).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(groupController.removeMember(1L, 2L, authentication).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        org.mockito.Mockito.verify(groupService).deleteGroup(1L, "alice");
        org.mockito.Mockito.verify(groupService).leaveGroup(1L, "alice");
        org.mockito.Mockito.verify(groupService).removeMember(1L, 2L, "alice");
    }
}

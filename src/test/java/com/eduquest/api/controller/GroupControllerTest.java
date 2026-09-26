package com.eduquest.api.controller;

import com.eduquest.api.dto.request.DocumentRequestDTO;
import com.eduquest.api.dto.request.GroupRequestDTO;
import com.eduquest.api.dto.response.DocumentResponseDTO;
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
        when(groupService.getAllGroups(any())).thenReturn(page);

        ResponseEntity<PageResponseDTO<GroupResponseDTO>> result = groupController.getAllGroups(0, 10);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().content()).hasSize(1);
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
}
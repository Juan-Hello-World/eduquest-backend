package com.eduquest.api.service.impl;

import com.eduquest.api.dto.request.DocumentRequestDTO;
import com.eduquest.api.dto.response.DocumentResponseDTO;
import com.eduquest.api.dto.response.PageResponseDTO;
import com.eduquest.api.entity.Document;
import com.eduquest.api.entity.PrivateGroup;
import com.eduquest.api.entity.User;
import com.eduquest.api.exception.ForbiddenException;
import com.eduquest.api.exception.ResourceNotFoundException;
import com.eduquest.api.repository.DocumentRepository;
import com.eduquest.api.repository.PrivateGroupRepository;
import com.eduquest.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DocumentServiceImplTest {

    @Mock DocumentRepository documentRepository;
    @Mock UserRepository userRepository;
    @Mock PrivateGroupRepository groupRepository;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks DocumentServiceImpl documentService;

    private User user;
    private Document document;
    private PrivateGroup group;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("alice");
        user.setEmail("alice@utec.edu.pe");

        document = new Document();
        document.setId(1L);
        document.setTitle("Apuntes de Calculo");
        document.setDescription("Semana 3");
        document.setFileUrl("https://cloud.example/calculo.pdf");
        document.setAuthor(user);

        group = new PrivateGroup();
        group.setId(1L);
        group.setName("Grupo de Calculo");
        group.getMembers().add(user);
    }

    @Test
    void createDocument_withExistingAuthor_persistsAndPublishesEvent() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> {
            Document saved = invocation.getArgument(0);
            saved.setId(7L);
            return saved;
        });

        DocumentRequestDTO request = new DocumentRequestDTO();
        request.setTitle(document.getTitle());
        request.setDescription(document.getDescription());
        request.setFileUrl(document.getFileUrl());

        DocumentResponseDTO response = documentService.createDocument(request, "alice");

        assertThat(response.getId()).isEqualTo(7L);
        assertThat(response.getTitle()).isEqualTo("Apuntes de Calculo");
        assertThat(response.getAuthorUsername()).isEqualTo("alice");
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    void createDocument_withUnknownAuthor_throwsNotFound() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        DocumentRequestDTO request = new DocumentRequestDTO();
        request.setTitle("x");
        request.setFileUrl("y");

        assertThatThrownBy(() -> documentService.createDocument(request, "ghost"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getAllDocuments_returnsPaginatedContent() {
        Pageable pageable = PageRequest.of(0, 10);
        when(documentRepository.findAll(pageable))
                .thenReturn(new PageImpl<>(List.of(document), pageable, 1));

        PageResponseDTO<DocumentResponseDTO> page = documentService.getAllDocuments(pageable);

        assertThat(page.content()).hasSize(1);
        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.page()).isZero();
        assertThat(page.totalPages()).isEqualTo(1);
    }

    @Test
    void uploadDocumentToGroup_asMember_createsDocumentInGroup() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(groupRepository.existsByIdAndMembersId(1L, 1L)).thenReturn(true);
        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> {
            Document saved = invocation.getArgument(0);
            saved.setId(8L);
            return saved;
        });

        DocumentRequestDTO request = new DocumentRequestDTO();
        request.setTitle("Tarea de grupo");
        request.setDescription("Semana 4");
        request.setFileUrl("https://cloud.example/tarea.pdf");

        DocumentResponseDTO response = documentService.uploadDocumentToGroup(1L, request, "alice");

        assertThat(response.getId()).isEqualTo(8L);
        assertThat(response.getGroupName()).isEqualTo("Grupo de Calculo");
        assertThat(response.getAuthorUsername()).isEqualTo("alice");
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    void uploadDocumentToGroup_withUnknownGroup_throwsNotFound() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(groupRepository.findById(99L)).thenReturn(Optional.empty());

        DocumentRequestDTO request = new DocumentRequestDTO();
        request.setTitle("x");
        request.setFileUrl("y");

        assertThatThrownBy(() -> documentService.uploadDocumentToGroup(99L, request, "alice"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void uploadDocumentToGroup_asNonMember_throwsForbidden() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group));
        when(groupRepository.existsByIdAndMembersId(1L, 1L)).thenReturn(false);

        DocumentRequestDTO request = new DocumentRequestDTO();
        request.setTitle("x");
        request.setFileUrl("y");

        assertThatThrownBy(() -> documentService.uploadDocumentToGroup(1L, request, "alice"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void uploadDocumentToGroup_withUnknownUser_throwsNotFound() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        DocumentRequestDTO request = new DocumentRequestDTO();
        request.setTitle("x");
        request.setFileUrl("y");

        assertThatThrownBy(() -> documentService.uploadDocumentToGroup(1L, request, "ghost"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
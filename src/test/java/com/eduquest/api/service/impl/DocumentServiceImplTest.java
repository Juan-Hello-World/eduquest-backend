package com.eduquest.api.service.impl;

import com.eduquest.api.dto.request.DocumentRequestDTO;
import com.eduquest.api.dto.response.DocumentResponseDTO;
import com.eduquest.api.dto.response.PageResponseDTO;
import com.eduquest.api.entity.Document;
import com.eduquest.api.entity.PrivateGroup;
import com.eduquest.api.entity.User;
import com.eduquest.api.event.OnDocumentUploadedEvent;
import com.eduquest.api.event.OnGroupActivityEvent;
import com.eduquest.api.event.OnUserActivityEvent;
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
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DocumentServiceImplTest {

    @Mock DocumentRepository documentRepository;
    @Mock UserRepository userRepository;
    @Mock PrivateGroupRepository groupRepository;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock com.eduquest.api.repository.TagRepository tagRepository;
    @Mock com.eduquest.api.repository.ReviewRepository reviewRepository;
    @Mock com.eduquest.api.service.FileStorageService fileStorageService;


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
        verify(eventPublisher).publishEvent(isA(OnDocumentUploadedEvent.class));
        verify(eventPublisher).publishEvent(isA(OnUserActivityEvent.class));
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
    void createDocument_withTags_resolvesAndSavesTags() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(tagRepository.findByName("Matematicas")).thenReturn(Optional.empty());
        when(tagRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

        DocumentRequestDTO request = new DocumentRequestDTO();
        request.setTitle("Calculo");
        request.setFileUrl("https://cloud.example/calc.pdf");
        request.setTags(java.util.Set.of("Matematicas"));

        DocumentResponseDTO response = documentService.createDocument(request, "alice");

        assertThat(response.getTags()).containsExactly("Matematicas");
    }

    @Test
    void getAllDocuments_returnsPaginatedContent() {
        Pageable pageable = PageRequest.of(0, 10);
        when(documentRepository.findByGroupIsNull(pageable))
                .thenReturn(new PageImpl<Document>(List.of(document), pageable, 1));

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
        verify(eventPublisher).publishEvent(isA(OnDocumentUploadedEvent.class));
        verify(eventPublisher).publishEvent(isA(OnGroupActivityEvent.class));
        verify(eventPublisher).publishEvent(isA(OnUserActivityEvent.class));
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

    @Test
    void getAllDocuments_withSearchAndTag_callsSearchPublicDocuments() {
        Pageable pageable = PageRequest.of(0, 10);
        when(documentRepository.searchPublicDocuments("%calculo%", "matematicas", null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(document), pageable, 1));

        PageResponseDTO<DocumentResponseDTO> page =
                documentService.getAllDocuments("Calculo", "Matematicas", pageable);

        assertThat(page.content()).hasSize(1);
    }

    @Test
    void getDocumentsByGroup_asMember_returnsPaginatedDocuments() {
        Pageable pageable = PageRequest.of(0, 10);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(groupRepository.existsById(1L)).thenReturn(true);
        when(groupRepository.existsByIdAndMembersId(1L, 1L)).thenReturn(true);
        when(documentRepository.findByGroupId(1L, pageable))
                .thenReturn(new PageImpl<Document>(List.of(document), pageable, 1));

        PageResponseDTO<DocumentResponseDTO> page =
                documentService.getDocumentsByGroup(1L, "alice", pageable);

        assertThat(page.content()).hasSize(1);
    }

    @Test
    void getDocumentsByGroup_asNonMember_throwsForbidden() {
        Pageable pageable = PageRequest.of(0, 10);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(groupRepository.existsById(1L)).thenReturn(true);
        when(groupRepository.existsByIdAndMembersId(1L, 1L)).thenReturn(false);

        assertThatThrownBy(() -> documentService.getDocumentsByGroup(1L, "alice", pageable))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void getAllDocuments_returnsPaginatedDtosWithNullAndPresentGroup() {
        Document inGroup = new Document();
        inGroup.setId(11L);
        inGroup.setTitle("Con grupo");
        inGroup.setFileUrl("https://cloud.example/g.pdf");
        inGroup.setAuthor(user);
        inGroup.setGroup(group);

        Pageable pageable = PageRequest.of(0, 10);
        when(documentRepository.findByGroupIsNull(pageable))
                .thenReturn(new PageImpl<Document>(List.of(document, inGroup), pageable, 2));

        PageResponseDTO<DocumentResponseDTO> page = documentService.getAllDocuments(pageable);

        assertThat(page.content()).hasSize(2);
        assertThat(page.content().get(0).getGroupName()).isNull();
        assertThat(page.content().get(1).getGroupName()).isEqualTo("Grupo de Calculo");
    }

    @Test
    void getAllDocuments_withCourseAndUniversity_buildsLikePatterns() {
        Pageable pageable = PageRequest.of(0, 10);
        when(documentRepository.searchPublicDocuments(null, null, "%calculo i%", "%utec%", pageable))
                .thenReturn(new PageImpl<>(List.of(document), pageable, 1));

        PageResponseDTO<DocumentResponseDTO> page =
                documentService.getAllDocuments(null, " ", "Calculo I", "UTEC", pageable);

        assertThat(page.content()).hasSize(1);
    }

    @Test
    void createDocument_storesCourseAndUniversity() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

        DocumentRequestDTO request = new DocumentRequestDTO();
        request.setTitle("Apuntes");
        request.setFileUrl("https://x/a.pdf");
        request.setCourse("  Calculo I ");
        request.setUniversity(" ");

        DocumentResponseDTO dto = documentService.createDocument(request, "alice");

        assertThat(dto.getCourse()).isEqualTo("Calculo I");
        assertThat(dto.getUniversity()).isNull();
    }

    @Test
    void saveDocument_publicDocument_addsToSavedList() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));

        documentService.saveDocument(1L, "alice");

        assertThat(user.getSavedDocuments()).contains(document);
        verify(userRepository).save(user);
    }

    @Test
    void saveDocument_groupDocumentWithoutMembership_throwsForbidden() {
        document.setGroup(group);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));
        when(groupRepository.existsByIdAndMembersId(1L, 1L)).thenReturn(false);

        assertThatThrownBy(() -> documentService.saveDocument(1L, "alice"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void saveDocument_missingDocument_throwsNotFound() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(documentRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.saveDocument(9L, "alice"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void unsaveDocument_removesOnlyWhenPresent() {
        user.getSavedDocuments().add(document);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        documentService.unsaveDocument(1L, "alice");
        documentService.unsaveDocument(1L, "alice");

        assertThat(user.getSavedDocuments()).isEmpty();
        verify(userRepository, org.mockito.Mockito.times(1)).save(user);
    }

    @Test
    void getSavedDocuments_andIds_useCurrentUser() {
        Pageable pageable = PageRequest.of(0, 10);
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(documentRepository.findSavedByUserId(1L, pageable)).thenReturn(new PageImpl<>(List.of(document), pageable, 1));
        when(documentRepository.findSavedIdsByUserId(1L)).thenReturn(List.of(1L));

        assertThat(documentService.getSavedDocuments("alice", pageable).content()).hasSize(1);
        assertThat(documentService.getSavedDocumentIds("alice")).containsExactly(1L);
    }

    @Test
    void deleteDocument_removesReviewsSavedEntriesAndDocument() {
        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));

        documentService.deleteDocument(1L);

        verify(reviewRepository).deleteByDocumentId(1L);
        verify(documentRepository).removeFromSavedLists(1L);
        verify(documentRepository).delete(document);
    }

    @Test
    void deleteDocument_missing_throwsNotFound() {
        when(documentRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.deleteDocument(9L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateDocument_byAuthor_updatesFieldsAndDropsOldFile() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));
        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> invocation.getArgument(0));
        DocumentRequestDTO request = new DocumentRequestDTO();
        request.setTitle(" Nuevo título ");
        request.setDescription(" ");
        request.setFileUrl("https://cloud.example/nuevo.pdf");
        request.setCourse("Cálculo");

        DocumentResponseDTO response = documentService.updateDocument(1L, request, "alice");

        assertThat(response.getTitle()).isEqualTo("Nuevo título");
        assertThat(response.getDescription()).isNull();
        assertThat(response.getCourse()).isEqualTo("Cálculo");
        verify(fileStorageService).deleteByUrl("https://cloud.example/calculo.pdf");
    }

    @Test
    void updateDocument_sameFile_keepsIt() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));
        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> invocation.getArgument(0));
        DocumentRequestDTO request = new DocumentRequestDTO();
        request.setTitle("Apuntes");
        request.setFileUrl("https://cloud.example/calculo.pdf");

        documentService.updateDocument(1L, request, "alice");

        org.mockito.Mockito.verify(fileStorageService, org.mockito.Mockito.never()).deleteByUrl(any());
    }

    @Test
    void deleteOwnDocument_authorOrAdminOnly() {
        User bob = new User();
        bob.setId(2L);
        bob.setUsername("bob");
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(bob));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));

        assertThatThrownBy(() -> documentService.deleteOwnDocument(1L, "bob")).isInstanceOf(ForbiddenException.class);

        com.eduquest.api.entity.Role admin = new com.eduquest.api.entity.Role();
        admin.setName(com.eduquest.api.entity.RoleName.ROLE_ADMIN);
        bob.getRoles().add(admin);
        documentService.deleteOwnDocument(1L, "bob");
        documentService.deleteOwnDocument(1L, "alice");

        verify(reviewRepository, org.mockito.Mockito.times(2)).deleteByDocumentId(1L);
        verify(fileStorageService, org.mockito.Mockito.times(2)).deleteByUrl("https://cloud.example/calculo.pdf");
    }

    @Test
    void setFeatured_onlyPublicMaterials() {
        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));
        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(documentService.setFeatured(1L, true).isFeatured()).isTrue();
        assertThat(documentService.setFeatured(1L, false).isFeatured()).isFalse();

        document.setGroup(group);
        assertThatThrownBy(() -> documentService.setFeatured(1L, true))
                .isInstanceOf(com.eduquest.api.exception.InvalidOperationException.class);
    }

    @Test
    void getFeaturedDocuments_mapsRepositoryResult() {
        document.setFeatured(true);
        when(documentRepository.findTop6ByFeaturedTrueAndGroupIsNullOrderByUploadedAtDesc()).thenReturn(List.of(document));

        assertThat(documentService.getFeaturedDocuments()).extracting(DocumentResponseDTO::isFeatured).containsExactly(true);
    }

    @Test
    void getPublicDocumentsByAuthor_returnsPageOrThrowsForUnknownUser() {
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 6);
        when(userRepository.existsById(1L)).thenReturn(true);
        when(documentRepository.findByAuthorIdAndGroupIsNull(1L, pageable))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(document), pageable, 1));

        assertThat(documentService.getPublicDocumentsByAuthor(1L, pageable).content())
                .extracting("title").containsExactly("Apuntes de Calculo");

        when(userRepository.existsById(9L)).thenReturn(false);
        assertThatThrownBy(() -> documentService.getPublicDocumentsByAuthor(9L, pageable))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}

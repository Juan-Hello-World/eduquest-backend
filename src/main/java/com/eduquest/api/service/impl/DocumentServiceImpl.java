package com.eduquest.api.service.impl;

import com.eduquest.api.dto.request.DocumentRequestDTO;
import com.eduquest.api.dto.response.DocumentResponseDTO;
import com.eduquest.api.dto.response.PageResponseDTO;
import com.eduquest.api.entity.Document;
import com.eduquest.api.entity.PrivateGroup;
import com.eduquest.api.entity.User;
import com.eduquest.api.event.OnDocumentUploadedEvent;
import com.eduquest.api.exception.ForbiddenException;
import com.eduquest.api.exception.ResourceNotFoundException;
import com.eduquest.api.repository.DocumentRepository;
import com.eduquest.api.repository.PrivateGroupRepository;
import com.eduquest.api.repository.UserRepository;
import com.eduquest.api.service.DocumentService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final PrivateGroupRepository groupRepository;
    private final ApplicationEventPublisher eventPublisher;

    public DocumentServiceImpl(DocumentRepository documentRepository, UserRepository userRepository,
                               PrivateGroupRepository groupRepository, ApplicationEventPublisher eventPublisher) {
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public DocumentResponseDTO createDocument(DocumentRequestDTO request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        Document document = new Document();
        document.setTitle(request.getTitle());
        document.setDescription(request.getDescription());
        document.setFileUrl(request.getFileUrl());
        document.setAuthor(user);

        Document savedDocument = documentRepository.save(document);

        eventPublisher.publishEvent(new OnDocumentUploadedEvent(
                this, savedDocument.getId(), savedDocument.getTitle(), user.getEmail()));

        return mapToResponseDTO(savedDocument);
    }

    @Override
    @Transactional
    public DocumentResponseDTO uploadDocumentToGroup(Long groupId, DocumentRequestDTO request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        PrivateGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Grupo no encontrado"));

        if (!groupRepository.existsByIdAndMembersId(groupId, user.getId())) {
            throw new ForbiddenException("Debes ser miembro del grupo para subir documentos");
        }

        Document document = new Document();
        document.setTitle(request.getTitle());
        document.setDescription(request.getDescription());
        document.setFileUrl(request.getFileUrl());
        document.setAuthor(user);
        document.setGroup(group);

        Document savedDocument = documentRepository.save(document);

        eventPublisher.publishEvent(new OnDocumentUploadedEvent(
                this, savedDocument.getId(), savedDocument.getTitle(), user.getEmail()));

        return mapToResponseDTO(savedDocument);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<DocumentResponseDTO> getAllDocuments(Pageable pageable) {
        Page<Document> documentPage = documentRepository.findAll(pageable);
        List<DocumentResponseDTO> content = documentPage.getContent().stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
        return PageResponseDTO.of(content, documentPage.getNumber(), documentPage.getSize(),
                documentPage.getTotalElements());
    }

    private DocumentResponseDTO mapToResponseDTO(Document document) {
        DocumentResponseDTO dto = new DocumentResponseDTO();
        dto.setId(document.getId());
        dto.setTitle(document.getTitle());
        dto.setDescription(document.getDescription());
        dto.setFileUrl(document.getFileUrl());
        dto.setUploadedAt(document.getUploadedAt());
        dto.setAuthorUsername(document.getAuthor().getUsername());
        dto.setGroupName(document.getGroup() != null ? document.getGroup().getName() : null);
        return dto;
    }
}
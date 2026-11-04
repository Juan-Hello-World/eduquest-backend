package com.eduquest.api.service.impl;

import com.eduquest.api.dto.request.DocumentRequestDTO;
import com.eduquest.api.dto.response.DocumentResponseDTO;
import com.eduquest.api.dto.response.PageResponseDTO;
import com.eduquest.api.entity.Document;
import com.eduquest.api.entity.PrivateGroup;
import com.eduquest.api.entity.Tag;
import com.eduquest.api.entity.User;
import com.eduquest.api.event.OnDocumentUploadedEvent;
import com.eduquest.api.exception.ForbiddenException;
import com.eduquest.api.exception.ResourceNotFoundException;
import com.eduquest.api.repository.DocumentRepository;
import com.eduquest.api.repository.PrivateGroupRepository;
import com.eduquest.api.repository.TagRepository;
import com.eduquest.api.repository.UserRepository;
import com.eduquest.api.service.DocumentService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final PrivateGroupRepository groupRepository;
    private final TagRepository tagRepository;
    private final ApplicationEventPublisher eventPublisher;

    public DocumentServiceImpl(DocumentRepository documentRepository,
                               UserRepository userRepository,
                               PrivateGroupRepository groupRepository,
                               TagRepository tagRepository,
                               ApplicationEventPublisher eventPublisher) {
        this.documentRepository = documentRepository;
        this.userRepository = userRepository;
        this.groupRepository = groupRepository;
        this.tagRepository=tagRepository;
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
        document.setTags(resolveTags(request.getTags()));

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
        document.setTags(resolveTags(request.getTags()));

        Document savedDocument = documentRepository.save(document);

        eventPublisher.publishEvent(new OnDocumentUploadedEvent(
                this, savedDocument.getId(), savedDocument.getTitle(), user.getEmail()));

        return mapToResponseDTO(savedDocument);
    }
    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<DocumentResponseDTO> getDocumentsByGroup(Long groupId, String username, Pageable pageable) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        if (!groupRepository.existsById(groupId)) {
            throw new ResourceNotFoundException("Grupo no encontrado");
        }

        if (!groupRepository.existsByIdAndMembersId(groupId, user.getId())) {
            throw new ForbiddenException("Debes ser miembro del grupo para ver sus documentos");
        }

        Page<Document> documentPage = documentRepository.findByGroupId(groupId, pageable);
        List<DocumentResponseDTO> content = documentPage.getContent().stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
        return PageResponseDTO.of(content, documentPage.getNumber(), documentPage.getSize(),
                documentPage.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<DocumentResponseDTO> getAllDocuments(Pageable pageable) {
        return getAllDocuments(null, null, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<DocumentResponseDTO> getAllDocuments(String search, String tag, Pageable pageable) {
        Page<Document> documentPage;
        if ((search == null || search.isBlank()) && (tag == null || tag.isBlank())) {
            documentPage = documentRepository.findAll(pageable);
        } else {
            documentPage = documentRepository.searchPublicDocuments(search, tag, pageable);
        }
        List<DocumentResponseDTO> content = documentPage.getContent().stream()
                .map(this::mapToResponseDTO)
                .collect(Collectors.toList());
        return PageResponseDTO.of(content, documentPage.getNumber(), documentPage.getSize(),
                documentPage.getTotalElements());
    }

    private Set<Tag> resolveTags(Set<String> tagNames) {
        Set<Tag> resolved = new HashSet<>();
        if (tagNames == null || tagNames.isEmpty()) {
            return resolved;
        }
        for (String rawName : tagNames) {
            if (rawName != null && !rawName.isBlank()) {
                String cleanName = rawName.trim();
                Tag tag = tagRepository.findByName(cleanName)
                        .orElseGet(() -> {
                            Tag newTag = new Tag();
                            newTag.setName(cleanName);
                            return tagRepository.save(newTag);
                        });
                resolved.add(tag);
            }
        }
        return resolved;
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
        Set<String> tagNames = document.getTags() == null ? Set.of() :
                document.getTags().stream().map(Tag::getName).collect(Collectors.toSet());
        dto.setTags(tagNames);
        return dto;
    }
}
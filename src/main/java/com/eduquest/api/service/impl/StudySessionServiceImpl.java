package com.eduquest.api.service.impl;

import com.eduquest.api.dto.request.StudySessionRequestDTO;
import com.eduquest.api.dto.response.StudySessionResponseDTO;
import com.eduquest.api.entity.PrivateGroup;
import com.eduquest.api.entity.StudySession;
import com.eduquest.api.exception.ResourceNotFoundException;
import com.eduquest.api.repository.PrivateGroupRepository;
import com.eduquest.api.repository.StudySessionRepository;
import com.eduquest.api.service.StudySessionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class StudySessionServiceImpl implements StudySessionService {

    private final StudySessionRepository sessionRepository;
    private final PrivateGroupRepository groupRepository;

    public StudySessionServiceImpl(StudySessionRepository sessionRepository, PrivateGroupRepository groupRepository) {
        this.sessionRepository = sessionRepository;
        this.groupRepository = groupRepository;
    }

    @Override
    @Transactional
    public StudySessionResponseDTO createSession(Long groupId, StudySessionRequestDTO request) {
        PrivateGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Grupo privado no encontrado"));

        StudySession session = new StudySession();
        session.setTitle(request.getTitle());
        session.setDescription(request.getDescription());
        session.setScheduledAt(request.getScheduledAt());
        session.setMeetingUrl(request.getMeetingUrl());

        // Usamos el nombre de campo exacto que tiene tu entidad StudySession
        session.setPrivateGroup(group);

        StudySession savedSession = sessionRepository.save(session);
        return mapToResponse(savedSession);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StudySessionResponseDTO> getSessionsByGroup(Long groupId) {
        if (!groupRepository.existsById(groupId)) {
            throw new ResourceNotFoundException("Grupo privado no encontrado");
        }

        // Asegúrate de que tu StudySessionRepository tenga un método para buscar por el ID del grupo privado
        return sessionRepository.findAll().stream()
                .filter(s -> s.getPrivateGroup() != null && s.getPrivateGroup().getId().equals(groupId))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private StudySessionResponseDTO mapToResponse(StudySession session) {
        StudySessionResponseDTO dto = new StudySessionResponseDTO();
        dto.setId(session.getId());
        dto.setTitle(session.getTitle());
        dto.setDescription(session.getDescription());
        dto.setScheduledAt(session.getScheduledAt());
        dto.setMeetingUrl(session.getMeetingUrl());
        dto.setGroupName(session.getPrivateGroup() != null ? session.getPrivateGroup().getName() : null);
        return dto;
    }
}
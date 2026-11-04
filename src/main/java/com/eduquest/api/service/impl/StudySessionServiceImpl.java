package com.eduquest.api.service.impl;

import com.eduquest.api.dto.request.StudySessionRequestDTO;
import com.eduquest.api.dto.response.StudySessionResponseDTO;
import com.eduquest.api.entity.PrivateGroup;
import com.eduquest.api.entity.StudySession;
import com.eduquest.api.entity.User;
import com.eduquest.api.exception.ForbiddenException;
import com.eduquest.api.exception.InvalidOperationException;
import com.eduquest.api.exception.ResourceNotFoundException;
import com.eduquest.api.repository.PrivateGroupRepository;
import com.eduquest.api.repository.StudySessionRepository;
import com.eduquest.api.repository.UserRepository;
import com.eduquest.api.service.StudySessionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StudySessionServiceImpl implements StudySessionService {

    private final StudySessionRepository sessionRepository;
    private final PrivateGroupRepository groupRepository;
    private final UserRepository userRepository;

    public StudySessionServiceImpl(StudySessionRepository sessionRepository,
                                   PrivateGroupRepository groupRepository,
                                   UserRepository userRepository) {
        this.sessionRepository = sessionRepository;
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public StudySessionResponseDTO scheduleSession(Long groupId, StudySessionRequestDTO request, String username) {

        if (request.getScheduledAt().isBefore(LocalDateTime.now())){
           throw new InvalidOperationException("No se puede programar una sesión de estudio en una fecha pasada. ");
        }
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));


        PrivateGroup group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Grupo privado no encontrado"));

        if (!groupRepository.existsByIdAndMembersId(groupId, user.getId())){
            throw new ForbiddenException("Debes ser miembro del grupo para programar sesiones de estudio");
        }

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
    public List<StudySessionResponseDTO> getSessionsByGroup(Long groupId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        if (!groupRepository.existsById(groupId)) {
            throw new ResourceNotFoundException("Grupo privado no encontrado");
        }

        if (!groupRepository.existsByIdAndMembersId(groupId, user.getId())){
            throw new ForbiddenException("Debes ser miembro del grupo para ver sus sesiones de estudio");
        }

        // Asegúrate de que tu StudySessionRepository tenga un método para buscar por el ID del grupo privado
        return sessionRepository.findByPrivateGroupId(groupId).stream()
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
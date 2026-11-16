package com.eduquest.api.service;

import com.eduquest.api.dto.response.StudySetResponseDTO;
import com.eduquest.api.entity.StudySetType;

public interface StudySetService {
    StudySetResponseDTO getStudySet(Long documentId, StudySetType type, boolean refresh, String username);
}

package com.eduquest.api.service;

import com.eduquest.api.dto.request.StudyRoomRequestDTO;
import com.eduquest.api.dto.response.StudyRoomDTO;

public interface StudyRoomService {
    StudyRoomDTO get(Long groupId, String username);
    StudyRoomDTO start(Long groupId, StudyRoomRequestDTO request, String username);
    StudyRoomDTO join(Long groupId, String username);
    StudyRoomDTO leave(Long groupId, String username);
    StudyRoomDTO stop(Long groupId, String username);
}

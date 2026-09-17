package com.eduquest.api.service;

import com.eduquest.api.dto.request.GroupRequestDTO;
import com.eduquest.api.dto.response.GroupResponseDTO;
import java.util.List;

public interface GroupService {
    GroupResponseDTO createGroup(GroupRequestDTO request, String username);
    List<GroupResponseDTO> getAllGroups();
}
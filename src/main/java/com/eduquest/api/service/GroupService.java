package com.eduquest.api.service;

import com.eduquest.api.dto.request.GroupRequestDTO;
import com.eduquest.api.dto.response.GroupResponseDTO;
import com.eduquest.api.dto.response.PageResponseDTO;
import org.springframework.data.domain.Pageable;

public interface GroupService {
    GroupResponseDTO createGroup(GroupRequestDTO request, String username);
    PageResponseDTO<GroupResponseDTO> getAllGroups(Pageable pageable);
}
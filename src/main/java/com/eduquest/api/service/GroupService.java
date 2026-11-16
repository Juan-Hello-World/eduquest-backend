package com.eduquest.api.service;

import com.eduquest.api.dto.request.GroupRequestDTO;
import com.eduquest.api.dto.request.GroupUpdateRequestDTO;
import com.eduquest.api.dto.response.GroupInviteResponseDTO;
import com.eduquest.api.dto.response.GroupMemberResponseDTO;
import com.eduquest.api.dto.response.GroupResponseDTO;
import com.eduquest.api.dto.response.PageResponseDTO;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface GroupService {
    GroupResponseDTO createGroup(GroupRequestDTO request, String username);
    PageResponseDTO<GroupResponseDTO> getAllGroups(Pageable pageable, String username, String search);
    List<GroupResponseDTO> getMyGroups(String username);
    void markAsSeen(Long groupId, String username);
    GroupResponseDTO getGroupById(Long groupId, String username);
    GroupResponseDTO getGroupByCode(String code, String username);
    GroupResponseDTO joinGroup(Long groupId, String username, String password, String inviteToken);

    default GroupResponseDTO joinGroup(Long groupId, String username, String password) {
        return joinGroup(groupId, username, password, null);
    }

    GroupInviteResponseDTO createInvite(Long groupId, String username, boolean regenerate);
    void changePassword(Long groupId, String newPassword, String username);
    List<GroupMemberResponseDTO> getMembers(Long groupId, String username);
    GroupResponseDTO updateGroup(Long groupId, GroupUpdateRequestDTO request, String username);
    void deleteGroup(Long groupId, String username);
    void leaveGroup(Long groupId, String username);
    void removeMember(Long groupId, Long memberId, String username);
}
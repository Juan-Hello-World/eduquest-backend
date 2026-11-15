package com.eduquest.api.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GroupMemberResponseDTO {
    private Long id;
    private String username;
    private String avatarUrl;
    private String groupRole;
    private boolean advisor;
}

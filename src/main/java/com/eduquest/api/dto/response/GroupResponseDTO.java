package com.eduquest.api.dto.response;
import com.eduquest.api.entity.GroupVisibility;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;


@Getter
@Setter
public class GroupResponseDTO {
    private Long id;
    private String name;
    private String description;
    private String creatorUsername;
    private String code;
    private GroupVisibility visibility;
    private boolean member;
    private int memberCount;
    private LocalDateTime lastActivityAt;
    private long unreadDocuments;
    private long unreadSessions;
    private long unreadCount;
    private long unreadMessages;
}

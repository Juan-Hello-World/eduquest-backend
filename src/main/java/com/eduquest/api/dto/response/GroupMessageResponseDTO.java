package com.eduquest.api.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class GroupMessageResponseDTO {
    private Long id;
    private String content;
    private String imageUrl;
    private Long authorId;
    private String authorUsername;
    private String authorAvatarUrl;
    private boolean authorAdvisor;
    private boolean mine;
    private LocalDateTime createdAt;
    private LocalDateTime editedAt;
    private boolean deleted;
    private MessageReplyDTO replyTo;
    private Long channelId;
    private Long threadRootId;
    private long threadReplyCount;
    private boolean pinned;
    private String fileUrl;
    private String fileName;
    private String sticker;
    private List<ReactionDTO> reactions = List.of();
    private PollResponseDTO poll;
    private boolean saved;
}

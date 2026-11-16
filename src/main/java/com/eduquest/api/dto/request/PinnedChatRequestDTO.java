package com.eduquest.api.dto.request;

import com.eduquest.api.entity.PinnedChatKind;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PinnedChatRequestDTO {
    @NotNull(message = "Indica el tipo de chat")
    private PinnedChatKind kind;

    @NotNull(message = "Indica el chat")
    private Long targetId;

    private boolean pinned;
}

package com.eduquest.api.dto.response;

import java.util.List;

public record ThreadResponseDTO(GroupMessageResponseDTO root, List<GroupMessageResponseDTO> replies) {
}

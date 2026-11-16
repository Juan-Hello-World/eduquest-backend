package com.eduquest.api.dto.response;

import java.util.List;

public record ReactionDTO(String emoji, int count, List<Long> userIds) {
}

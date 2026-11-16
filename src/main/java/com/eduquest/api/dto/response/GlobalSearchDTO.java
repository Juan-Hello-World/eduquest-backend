package com.eduquest.api.dto.response;

import java.util.List;

public record GlobalSearchDTO(List<SearchHitDTO> documents, List<SearchHitDTO> groups, List<SearchHitDTO> questions,
                              List<UserSummaryDTO> users) {
}

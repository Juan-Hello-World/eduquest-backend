package com.eduquest.api.dto.response;

import java.util.List;

public record PollResponseDTO(Long id, String question, List<Option> options, int totalVotes) {

    public record Option(Long id, String text, int votes, List<Long> voterIds) {
    }
}

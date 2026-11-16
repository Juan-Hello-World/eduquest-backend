package com.eduquest.api.service;

import com.eduquest.api.dto.request.BanAppealRequestDTO;
import com.eduquest.api.dto.response.BanAppealResponseDTO;
import com.eduquest.api.dto.response.PageResponseDTO;
import org.springframework.data.domain.Pageable;

public interface AppealService {
    BanAppealResponseDTO submit(BanAppealRequestDTO request);
    PageResponseDTO<BanAppealResponseDTO> list(String status, Pageable pageable);
    BanAppealResponseDTO decide(Long appealId, boolean accepted, String response, String moderatorUsername);
    long countPending();
}

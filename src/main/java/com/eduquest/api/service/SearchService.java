package com.eduquest.api.service;

import com.eduquest.api.dto.response.GlobalSearchDTO;

public interface SearchService {
    GlobalSearchDTO search(String query, String username);
}

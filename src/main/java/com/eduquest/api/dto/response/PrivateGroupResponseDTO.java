package com.eduquest.api.dto.response;

import lombok.Data;

@Data
public class PrivateGroupResponseDTO {
    private Long id;
    private String name;
    private String description;
    private int memberCount;
}
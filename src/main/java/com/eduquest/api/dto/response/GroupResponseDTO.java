package com.eduquest.api.dto.response;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class GroupResponseDTO {
    private Long id;
    private String name;
    private String description;
    private String creatorUsername;
}
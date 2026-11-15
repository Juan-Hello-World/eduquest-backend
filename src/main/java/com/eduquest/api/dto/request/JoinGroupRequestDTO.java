package com.eduquest.api.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JoinGroupRequestDTO {
    private String password;

    private String inviteToken;
}

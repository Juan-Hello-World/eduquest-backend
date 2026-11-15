package com.eduquest.api.exception;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class UserBannedException extends RuntimeException {

    public static final String CODE = "USER_BANNED";

    private final String reason;
    private final LocalDateTime bannedAt;

    public UserBannedException(String reason, LocalDateTime bannedAt) {
        super("Tu cuenta fue suspendida por incumplir las normas de EduQuest");
        this.reason = reason;
        this.bannedAt = bannedAt;
    }
}

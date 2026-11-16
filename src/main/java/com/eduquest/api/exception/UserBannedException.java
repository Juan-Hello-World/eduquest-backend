package com.eduquest.api.exception;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class UserBannedException extends RuntimeException {

    public static final String CODE = "USER_BANNED";

    private final String reason;
    private final LocalDateTime bannedAt;
    private final LocalDateTime bannedUntil;

    public UserBannedException(String reason, LocalDateTime bannedAt) {
        this(reason, bannedAt, null);
    }

    public UserBannedException(String reason, LocalDateTime bannedAt, LocalDateTime bannedUntil) {
        super("Tu cuenta fue suspendida por incumplir las normas de EduQuest");
        this.reason = reason;
        this.bannedAt = bannedAt;
        this.bannedUntil = bannedUntil;
    }
}

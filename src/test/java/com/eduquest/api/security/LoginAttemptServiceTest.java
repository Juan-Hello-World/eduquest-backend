package com.eduquest.api.security;

import com.eduquest.api.exception.TooManyAttemptsException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoginAttemptServiceTest {

    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-11-15T12:00:00Z");

        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    @Test
    void locksAfterMaxFailures_andUnlocksAfterDuration() {
        MutableClock clock = new MutableClock();
        LoginAttemptService service = new LoginAttemptService(3, Duration.ofMinutes(15), clock);

        service.recordFailure("Alice");
        service.recordFailure("alice ");
        assertThat(service.remainingAttempts("ALICE")).isEqualTo(1);
        assertThatCode(() -> service.ensureAllowed("alice")).doesNotThrowAnyException();

        service.recordFailure("alice");
        assertThatThrownBy(() -> service.ensureAllowed("alice"))
                .isInstanceOf(TooManyAttemptsException.class)
                .hasMessageContaining("15 minutos");

        clock.now = clock.now.plus(Duration.ofMinutes(14)).plusSeconds(30);
        assertThatThrownBy(() -> service.ensureAllowed("alice")).hasMessageContaining("1 minuto");

        clock.now = clock.now.plus(Duration.ofMinutes(1));
        assertThatCode(() -> service.ensureAllowed("alice")).doesNotThrowAnyException();
        assertThat(service.remainingAttempts("alice")).isEqualTo(3);
    }

    @Test
    void successClearsFailures_andExpiredLockRestartsCount() {
        MutableClock clock = new MutableClock();
        LoginAttemptService service = new LoginAttemptService(2, Duration.ofMinutes(1), clock);

        service.recordFailure("bob");
        service.recordSuccess("bob");
        assertThat(service.remainingAttempts("bob")).isEqualTo(2);

        service.recordFailure("bob");
        service.recordFailure("bob");
        clock.now = clock.now.plus(Duration.ofMinutes(2));
        service.recordFailure("bob");
        assertThat(service.remainingAttempts("bob")).isEqualTo(1);
        assertThatCode(() -> service.ensureAllowed(null)).doesNotThrowAnyException();
        assertThat(new LoginAttemptService(5, 15).remainingAttempts("x")).isEqualTo(5);
    }
}

package com.eduquest.api.security;

import com.eduquest.api.exception.TooManyAttemptsException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class LoginAttemptService {

    private final Map<String, Attempts> attempts = new ConcurrentHashMap<>();
    private final int maxFailures;
    private final Duration lockDuration;
    private final Clock clock;

    @Autowired
    public LoginAttemptService(@Value("${security.login.max-failures:5}") int maxFailures,
                               @Value("${security.login.lock-minutes:15}") long lockMinutes) {
        this(maxFailures, Duration.ofMinutes(lockMinutes), Clock.systemUTC());
    }

    LoginAttemptService(int maxFailures, Duration lockDuration, Clock clock) {
        this.maxFailures = maxFailures;
        this.lockDuration = lockDuration;
        this.clock = clock;
    }

    public void ensureAllowed(String username) {
        Attempts current = attempts.get(key(username));
        if (current == null || current.lockedUntil == null) {
            return;
        }
        Instant now = clock.instant();
        if (current.lockedUntil.isAfter(now)) {
            long seconds = Math.max(1, Duration.between(now, current.lockedUntil).toSeconds());
            long minutes = (seconds + 59) / 60;
            throw new TooManyAttemptsException("Demasiados intentos fallidos. Vuelve a intentarlo en "
                    + minutes + (minutes == 1 ? " minuto" : " minutos"), seconds);
        }
        attempts.remove(key(username));
    }

    public void recordFailure(String username) {
        Instant now = clock.instant();
        attempts.compute(key(username), (name, current) -> {
            Attempts next = current == null || (current.lockedUntil != null && !current.lockedUntil.isAfter(now))
                    ? new Attempts() : current;
            next.failures++;
            if (next.failures >= maxFailures) {
                next.lockedUntil = now.plus(lockDuration);
            }
            return next;
        });
    }

    public void recordSuccess(String username) {
        attempts.remove(key(username));
    }

    public int remainingAttempts(String username) {
        Attempts current = attempts.get(key(username));
        return current == null ? maxFailures : Math.max(0, maxFailures - current.failures);
    }

    private static String key(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }

    private static final class Attempts {
        private int failures;
        private Instant lockedUntil;
    }
}

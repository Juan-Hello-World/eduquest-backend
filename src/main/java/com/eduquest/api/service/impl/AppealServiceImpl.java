package com.eduquest.api.service.impl;

import com.eduquest.api.dto.request.BanAppealRequestDTO;
import com.eduquest.api.dto.response.BanAppealResponseDTO;
import com.eduquest.api.dto.response.PageResponseDTO;
import com.eduquest.api.entity.AppealStatus;
import com.eduquest.api.entity.BanAppeal;
import com.eduquest.api.entity.RoleName;
import com.eduquest.api.entity.User;
import com.eduquest.api.event.OnAccountEmailEvent;
import com.eduquest.api.exception.BadRequestException;
import com.eduquest.api.exception.DuplicateResourceException;
import com.eduquest.api.exception.ForbiddenException;
import com.eduquest.api.exception.InvalidCredentialsException;
import com.eduquest.api.exception.ResourceNotFoundException;
import com.eduquest.api.repository.BanAppealRepository;
import com.eduquest.api.repository.UserRepository;
import com.eduquest.api.security.LoginAttemptService;
import com.eduquest.api.service.AppealService;
import com.eduquest.api.service.NotificationService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;

@Service
public class AppealServiceImpl implements AppealService {

    private final BanAppealRepository appealRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final NotificationService notificationService;
    private final LoginAttemptService loginAttemptService;
    private final ApplicationEventPublisher eventPublisher;

    public AppealServiceImpl(BanAppealRepository appealRepository,
                             UserRepository userRepository,
                             PasswordEncoder passwordEncoder,
                             NotificationService notificationService,
                             LoginAttemptService loginAttemptService,
                             ApplicationEventPublisher eventPublisher) {
        this.appealRepository = appealRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.notificationService = notificationService;
        this.loginAttemptService = loginAttemptService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public BanAppealResponseDTO submit(BanAppealRequestDTO request) {
        String username = request.getUsername().trim();
        loginAttemptService.ensureAllowed(username);
        User user = userRepository.findByUsername(username)
                .filter(found -> passwordEncoder.matches(request.getPassword(), found.getPassword()))
                .orElse(null);
        if (user == null) {
            loginAttemptService.recordFailure(username);
            throw new InvalidCredentialsException("Usuario o contraseña incorrectos");
        }
        loginAttemptService.recordSuccess(username);
        if (!user.isBanned()) {
            throw new BadRequestException("Tu cuenta no está suspendida");
        }
        if (appealRepository.existsByUserIdAndStatus(user.getId(), AppealStatus.PENDING)) {
            throw new DuplicateResourceException("Ya enviaste una apelación; la moderación la está revisando");
        }

        BanAppeal appeal = new BanAppeal();
        appeal.setUser(user);
        appeal.setMessage(request.getMessage().trim());
        appeal.setBanReason(user.getBanReason());
        appeal.setBannedUntil(user.getBannedUntil());
        appeal.setCreatedAt(LocalDateTime.now());
        return toResponse(appealRepository.save(appeal));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<BanAppealResponseDTO> list(String status, Pageable pageable) {
        Page<BanAppeal> page = status == null || status.isBlank()
                ? appealRepository.findAllByOrderByCreatedAtDesc(pageable)
                : appealRepository.findByStatusOrderByCreatedAtDesc(parseStatus(status), pageable);
        return PageResponseDTO.of(page.getContent().stream().map(AppealServiceImpl::toResponse).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Override
    @Transactional
    public BanAppealResponseDTO decide(Long appealId, boolean accepted, String response, String moderatorUsername) {
        User moderator = userRepository.findByUsername(moderatorUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        BanAppeal appeal = appealRepository.findById(appealId)
                .orElseThrow(() -> new ResourceNotFoundException("Apelación no encontrada"));
        if (appeal.getStatus() != AppealStatus.PENDING) {
            throw new BadRequestException("Esta apelación ya fue revisada");
        }
        User user = appeal.getUser();
        if (!moderator.hasRole(RoleName.ROLE_ADMIN) && user.isBanned() && user.getBannedUntil() == null) {
            throw new ForbiddenException("Solo un administrador puede revisar un veto permanente");
        }

        String cleanResponse = response == null || response.isBlank() ? null : response.trim();
        appeal.setStatus(accepted ? AppealStatus.ACCEPTED : AppealStatus.REJECTED);
        appeal.setReviewedBy(moderator);
        appeal.setReviewedAt(LocalDateTime.now());
        appeal.setResponse(cleanResponse);
        if (accepted) {
            user.setBanned(false);
            user.setBanReason(null);
            user.setBannedAt(null);
            user.setBannedUntil(null);
            userRepository.save(user);
        }
        notificationService.notifyAppealDecision(user, accepted, cleanResponse);
        eventPublisher.publishEvent(new OnAccountEmailEvent(this,
                accepted ? OnAccountEmailEvent.Kind.APPEAL_ACCEPTED : OnAccountEmailEvent.Kind.APPEAL_REJECTED,
                user.getEmail(), user.getUsername(), cleanResponse));
        return toResponse(appealRepository.save(appeal));
    }

    @Override
    @Transactional(readOnly = true)
    public long countPending() {
        return appealRepository.countByStatus(AppealStatus.PENDING);
    }

    private static AppealStatus parseStatus(String status) {
        try {
            return AppealStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Estado de apelación no válido");
        }
    }

    private static BanAppealResponseDTO toResponse(BanAppeal appeal) {
        User user = appeal.getUser();
        return new BanAppealResponseDTO(appeal.getId(), user.getId(), user.getUsername(), appeal.getBanReason(),
                appeal.getBannedUntil(), appeal.getMessage(), appeal.getStatus().name(), appeal.getCreatedAt(),
                appeal.getReviewedBy() != null ? appeal.getReviewedBy().getUsername() : null,
                appeal.getReviewedAt(), appeal.getResponse());
    }
}

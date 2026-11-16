package com.eduquest.api.service.impl;

import com.eduquest.api.dto.request.BanAppealRequestDTO;
import com.eduquest.api.dto.response.BanAppealResponseDTO;
import com.eduquest.api.entity.AppealStatus;
import com.eduquest.api.entity.BanAppeal;
import com.eduquest.api.entity.Role;
import com.eduquest.api.entity.RoleName;
import com.eduquest.api.entity.User;
import com.eduquest.api.event.OnAccountEmailEvent;
import com.eduquest.api.exception.BadRequestException;
import com.eduquest.api.exception.DuplicateResourceException;
import com.eduquest.api.exception.ForbiddenException;
import com.eduquest.api.exception.InvalidCredentialsException;
import com.eduquest.api.repository.BanAppealRepository;
import com.eduquest.api.repository.UserRepository;
import com.eduquest.api.security.LoginAttemptService;
import com.eduquest.api.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AppealServiceImplTest {

    @Mock BanAppealRepository appealRepository;
    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock NotificationService notificationService;
    @Mock LoginAttemptService loginAttemptService;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks AppealServiceImpl appealService;

    private User banned;
    private User manager;

    @BeforeEach
    void setUp() {
        banned = new User();
        banned.setId(2L);
        banned.setUsername("bob");
        banned.setEmail("bob@utec.edu.pe");
        banned.setPassword("hash");
        banned.setBanned(true);
        banned.setBanReason("Spam");
        banned.setBannedUntil(LocalDateTime.now().plusDays(3));
        manager = new User();
        manager.setId(9L);
        manager.setUsername("gestor");
        manager.setRoles(new HashSet<>(Set.of(new Role(3L, RoleName.ROLE_MANAGER))));
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(banned));
        when(userRepository.findByUsername("gestor")).thenReturn(Optional.of(manager));
        when(passwordEncoder.matches("clave", "hash")).thenReturn(true);
        when(appealRepository.save(any(BanAppeal.class))).thenAnswer(invocation -> {
            BanAppeal appeal = invocation.getArgument(0);
            if (appeal.getId() == null) {
                appeal.setId(1L);
            }
            return appeal;
        });
    }

    private BanAppealRequestDTO request(String password) {
        BanAppealRequestDTO request = new BanAppealRequestDTO();
        request.setUsername(" bob ");
        request.setPassword(password);
        request.setMessage("  Fue un malentendido, no volverá a pasar.  ");
        return request;
    }

    @Test
    void submit_withValidCredentials_createsPendingAppeal() {
        BanAppealResponseDTO response = appealService.submit(request("clave"));

        assertThat(response.status()).isEqualTo("PENDING");
        assertThat(response.banReason()).isEqualTo("Spam");
        assertThat(response.message()).isEqualTo("Fue un malentendido, no volverá a pasar.");
        verify(loginAttemptService).recordSuccess("bob");
    }

    @Test
    void submit_rejectsWrongPasswordDuplicatesAndActiveAccounts() {
        assertThatThrownBy(() -> appealService.submit(request("mala"))).isInstanceOf(InvalidCredentialsException.class);
        verify(loginAttemptService).recordFailure("bob");

        when(appealRepository.existsByUserIdAndStatus(2L, AppealStatus.PENDING)).thenReturn(true);
        assertThatThrownBy(() -> appealService.submit(request("clave"))).isInstanceOf(DuplicateResourceException.class);

        banned.setBanned(false);
        assertThatThrownBy(() -> appealService.submit(request("clave"))).isInstanceOf(BadRequestException.class);
    }

    private BanAppeal pending() {
        BanAppeal appeal = new BanAppeal();
        appeal.setId(5L);
        appeal.setUser(banned);
        appeal.setMessage("Perdón");
        appeal.setCreatedAt(LocalDateTime.now());
        when(appealRepository.findById(5L)).thenReturn(Optional.of(appeal));
        return appeal;
    }

    @Test
    void decide_accept_unbansNotifiesAndEmails() {
        pending();

        BanAppealResponseDTO response = appealService.decide(5L, true, "  Bienvenido de vuelta ", "gestor");

        assertThat(response.status()).isEqualTo("ACCEPTED");
        assertThat(response.reviewedBy()).isEqualTo("gestor");
        assertThat(banned.isBanned()).isFalse();
        verify(notificationService).notifyAppealDecision(banned, true, "Bienvenido de vuelta");
        verify(eventPublisher).publishEvent(isA(OnAccountEmailEvent.class));
    }

    @Test
    void decide_reject_keepsBan_andCannotDecideTwice() {
        BanAppeal appeal = pending();

        assertThat(appealService.decide(5L, false, " ", "gestor").status()).isEqualTo("REJECTED");
        assertThat(banned.isBanned()).isTrue();
        assertThat(appeal.getResponse()).isNull();
        assertThatThrownBy(() -> appealService.decide(5L, true, null, "gestor")).isInstanceOf(BadRequestException.class);
    }

    @Test
    void decide_permanentBan_requiresAdmin() {
        banned.setBannedUntil(null);
        pending();

        assertThatThrownBy(() -> appealService.decide(5L, true, null, "gestor")).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void list_andCountPending() {
        BanAppeal appeal = pending();
        when(appealRepository.findAllByOrderByCreatedAtDesc(any())).thenReturn(new PageImpl<>(List.of(appeal)));
        when(appealRepository.findByStatusOrderByCreatedAtDesc(AppealStatus.PENDING, PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(appeal)));
        when(appealRepository.countByStatus(AppealStatus.PENDING)).thenReturn(1L);

        assertThat(appealService.list(null, PageRequest.of(0, 10)).content()).hasSize(1);
        assertThat(appealService.list("pending", PageRequest.of(0, 10)).content()).hasSize(1);
        assertThatThrownBy(() -> appealService.list("otro", PageRequest.of(0, 10))).isInstanceOf(BadRequestException.class);
        assertThat(appealService.countPending()).isEqualTo(1);
    }
}

package com.eduquest.api.controller;

import com.eduquest.api.dto.request.AppealDecisionRequestDTO;
import com.eduquest.api.dto.request.BanAppealRequestDTO;
import com.eduquest.api.dto.request.VerifyEmailRequestDTO;
import com.eduquest.api.dto.response.BanAppealResponseDTO;
import com.eduquest.api.dto.response.PageResponseDTO;
import com.eduquest.api.dto.response.UserResponseDTO;
import com.eduquest.api.security.UserPrincipal;
import com.eduquest.api.service.AppealService;
import com.eduquest.api.service.EmailVerificationService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AccountControllersTest {

    @Test
    void appealController_delegatesToService() {
        AppealService service = mock(AppealService.class);
        AppealController controller = new AppealController(service);
        BanAppealResponseDTO appeal = new BanAppealResponseDTO(1L, 2L, "bob", "Spam", null, "Perdón", "PENDING",
                null, null, null, null);
        BanAppealRequestDTO request = new BanAppealRequestDTO();
        AppealDecisionRequestDTO decision = new AppealDecisionRequestDTO();
        decision.setAccepted(true);
        decision.setResponse("Ok");
        when(service.submit(request)).thenReturn(appeal);
        when(service.list("PENDING", PageRequest.of(0, 10))).thenReturn(PageResponseDTO.of(List.of(appeal), 0, 10, 1));
        when(service.countPending()).thenReturn(3L);
        when(service.decide(1L, true, "Ok", "gestor")).thenReturn(appeal);

        assertThat(controller.submit(request).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(controller.list("PENDING", PageRequest.of(0, 10)).getBody().content()).hasSize(1);
        assertThat(controller.countPending().getBody()).containsEntry("count", 3L);
        assertThat(controller.decide(1L, decision, new UsernamePasswordAuthenticationToken("gestor", null)).getBody())
                .isSameAs(appeal);
    }

    @Test
    void emailVerificationController_delegatesToService() {
        EmailVerificationService service = mock(EmailVerificationService.class);
        EmailVerificationController controller = new EmailVerificationController(service);
        UserPrincipal principal = new UserPrincipal(1L, "alice", "alice@utec.edu.pe", "x", List.of());
        VerifyEmailRequestDTO request = new VerifyEmailRequestDTO();
        request.setCode("123456");
        UserResponseDTO user = new UserResponseDTO();
        when(service.verify("alice", "123456")).thenReturn(user);

        assertThat(controller.verify(request, principal).getBody()).isSameAs(user);
        assertThat(controller.resend(principal).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(service).resend("alice");
    }
}

package com.eduquest.api.controller;

import com.eduquest.api.dto.request.ForumAnswerRequestDTO;
import com.eduquest.api.dto.request.ForumQuestionRequestDTO;
import com.eduquest.api.dto.request.ForumVoteRequestDTO;
import com.eduquest.api.dto.response.ForumAnswerResponseDTO;
import com.eduquest.api.dto.response.ForumQuestionResponseDTO;
import com.eduquest.api.dto.response.ForumThreadResponseDTO;
import com.eduquest.api.dto.response.PageResponseDTO;
import com.eduquest.api.service.ForumService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ForumControllerTest {

    private final ForumService service = mock(ForumService.class);
    private final ForumController controller = new ForumController(service);
    private final UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken("alice", null);

    @Test
    void endpoints_delegateToService() {
        ForumQuestionResponseDTO question = new ForumQuestionResponseDTO(1L, "T", "B", null, null, null, 0, false, true, true);
        ForumAnswerResponseDTO answer = new ForumAnswerResponseDTO(2L, "R", null, null, 0, 0, false, true, true);
        ForumThreadResponseDTO thread = new ForumThreadResponseDTO(question, List.of(answer), true);
        ForumQuestionRequestDTO ask = new ForumQuestionRequestDTO();
        ForumAnswerRequestDTO reply = new ForumAnswerRequestDTO();
        reply.setBody("R");
        ForumVoteRequestDTO vote = new ForumVoteRequestDTO();
        vote.setValue(1);
        PageRequest pageable = PageRequest.of(0, 10);
        when(service.search("alice", "x", "c", "all", pageable)).thenReturn(PageResponseDTO.of(List.of(question), 0, 10, 1));
        when(service.ask("alice", ask)).thenReturn(question);
        when(service.getThread("alice", 1L)).thenReturn(thread);
        when(service.answer("alice", 1L, "R")).thenReturn(answer);
        when(service.accept("alice", 1L, 2L)).thenReturn(thread);
        when(service.vote("alice", 2L, 1)).thenReturn(answer);

        assertThat(controller.search("x", "c", "all", pageable, auth).getBody().content()).hasSize(1);
        assertThat(controller.ask(ask, auth).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(controller.getThread(1L, auth).getBody()).isSameAs(thread);
        assertThat(controller.answer(1L, reply, auth).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(controller.accept(1L, 2L, auth).getBody()).isSameAs(thread);
        assertThat(controller.vote(2L, vote, auth).getBody()).isSameAs(answer);
        assertThat(controller.deleteQuestion(1L, auth).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(controller.deleteAnswer(2L, auth).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(service).deleteQuestion("alice", 1L);
        verify(service).deleteAnswer("alice", 2L);
    }
}

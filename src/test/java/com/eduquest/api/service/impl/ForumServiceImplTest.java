package com.eduquest.api.service.impl;

import com.eduquest.api.dto.request.ForumQuestionRequestDTO;
import com.eduquest.api.dto.response.ForumAnswerResponseDTO;
import com.eduquest.api.dto.response.ForumQuestionResponseDTO;
import com.eduquest.api.dto.response.ForumThreadResponseDTO;
import com.eduquest.api.entity.ForumAnswer;
import com.eduquest.api.entity.ForumQuestion;
import com.eduquest.api.entity.ForumVote;
import com.eduquest.api.entity.Role;
import com.eduquest.api.entity.RoleName;
import com.eduquest.api.entity.User;
import com.eduquest.api.exception.BadRequestException;
import com.eduquest.api.exception.ForbiddenException;
import com.eduquest.api.exception.ResourceNotFoundException;
import com.eduquest.api.repository.ForumAnswerRepository;
import com.eduquest.api.repository.ForumQuestionRepository;
import com.eduquest.api.repository.ForumVoteRepository;
import com.eduquest.api.repository.UserRepository;
import com.eduquest.api.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ForumServiceImplTest {

    @Mock ForumQuestionRepository questionRepository;
    @Mock ForumAnswerRepository answerRepository;
    @Mock ForumVoteRepository voteRepository;
    @Mock UserRepository userRepository;
    @Mock NotificationService notificationService;

    @InjectMocks ForumServiceImpl forumService;

    private User alice;
    private User bob;
    private ForumQuestion question;
    private ForumAnswer answer;

    private static User user(long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setRoles(new HashSet<>());
        return user;
    }

    @BeforeEach
    void setUp() {
        alice = user(1L, "alice");
        bob = user(2L, "bob");
        question = new ForumQuestion();
        question.setId(10L);
        question.setTitle("¿Cómo integro por partes?");
        question.setBody("No entiendo cuándo usar LIATE");
        question.setAuthor(alice);
        question.setCreatedAt(LocalDateTime.now());
        answer = new ForumAnswer();
        answer.setId(20L);
        answer.setQuestion(question);
        answer.setAuthor(bob);
        answer.setBody("Elige u con LIATE");
        answer.setCreatedAt(LocalDateTime.now());
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(bob));
        when(questionRepository.findById(10L)).thenReturn(Optional.of(question));
        when(answerRepository.findById(20L)).thenReturn(Optional.of(answer));
        when(questionRepository.save(any(ForumQuestion.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(answerRepository.save(any(ForumAnswer.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void search_buildsPatternAndCountsAnswers() {
        when(questionRepository.search(anyString(), anyString(), anyString(), any()))
                .thenReturn(new PageImpl<>(List.of(question), PageRequest.of(0, 10), 1));
        when(answerRepository.countByQuestionIds(List.of(10L))).thenReturn(List.<Object[]>of(new Object[]{10L, 3L}));

        var page = forumService.search("bob", " Integral_%", " Cálculo ", "unanswered", PageRequest.of(0, 10));

        assertThat(page.content()).hasSize(1);
        assertThat(page.content().get(0).answerCount()).isEqualTo(3);
        assertThat(page.content().get(0).mine()).isFalse();
        verify(questionRepository).search(eq("%integral\\_\\%%"), eq("cálculo"), eq("unanswered"), any());
        assertThatThrownBy(() -> forumService.search("bob", null, null, "otro", PageRequest.of(0, 10)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void search_emptyPage_skipsCounts() {
        when(questionRepository.search(anyString(), anyString(), anyString(), any())).thenReturn(new PageImpl<>(List.of()));

        assertThat(forumService.search("bob", null, null, null, PageRequest.of(0, 10)).content()).isEmpty();
        verify(answerRepository, never()).countByQuestionIds(any());
    }

    @Test
    void ask_trimsAndStores() {
        ForumQuestionRequestDTO request = new ForumQuestionRequestDTO();
        request.setTitle("  ¿Qué es un espacio vectorial?  ");
        request.setBody("  Necesito un ejemplo sencillo  ");
        request.setCourse("  ");

        ForumQuestionResponseDTO response = forumService.ask("alice", request);

        assertThat(response.title()).isEqualTo("¿Qué es un espacio vectorial?");
        assertThat(response.course()).isNull();
        assertThat(response.mine()).isTrue();
        request.setCourse(" Álgebra ");
        assertThat(forumService.ask("alice", request).course()).isEqualTo("Álgebra");
    }

    @Test
    void answer_notifiesQuestionAuthorExceptSelf() {
        ForumAnswerResponseDTO response = forumService.answer("bob", 10L, " Usa LIATE ");

        assertThat(response.body()).isEqualTo("Usa LIATE");
        verify(notificationService).notifyForumActivity(eq(alice), anyString(), anyString(), eq(10L));

        forumService.answer("alice", 10L, "Gracias");
        verify(notificationService, org.mockito.Mockito.times(1)).notifyForumActivity(any(), anyString(), anyString(), any());
    }

    @Test
    void getThread_sortsAcceptedFirstAndIncludesVotes() {
        ForumAnswer other = new ForumAnswer();
        other.setId(21L);
        other.setQuestion(question);
        other.setAuthor(alice);
        other.setBody("Otra");
        other.setCreatedAt(LocalDateTime.now());
        question.setAcceptedAnswerId(20L);
        when(answerRepository.findByQuestionIdOrderByCreatedAtAsc(10L)).thenReturn(List.of(other, answer));
        when(voteRepository.sumByQuestion(10L)).thenReturn(List.<Object[]>of(new Object[]{21L, 5L}));
        ForumVote vote = new ForumVote();
        vote.setAnswer(other);
        vote.setValue(1);
        when(voteRepository.findByQuestionAndUser(10L, 2L)).thenReturn(List.of(vote));

        ForumThreadResponseDTO thread = forumService.getThread("bob", 10L);

        assertThat(thread.answers()).extracting(ForumAnswerResponseDTO::id).containsExactly(20L, 21L);
        assertThat(thread.answers().get(1).score()).isEqualTo(5);
        assertThat(thread.answers().get(1).myVote()).isEqualTo(1);
        assertThat(thread.question().solved()).isTrue();
        assertThat(thread.canAccept()).isFalse();
    }

    @Test
    void vote_upsertsRemovesAndRejectsOwnAnswer() {
        when(voteRepository.findByAnswerIdAndUserId(20L, 1L)).thenReturn(Optional.empty());
        when(voteRepository.scoreOf(20L)).thenReturn(1L);

        assertThat(forumService.vote("alice", 20L, 5).myVote()).isEqualTo(1);
        verify(voteRepository, org.mockito.Mockito.times(1)).save(any(ForumVote.class));

        ForumVote existing = new ForumVote();
        when(voteRepository.findByAnswerIdAndUserId(20L, 1L)).thenReturn(Optional.of(existing));
        forumService.vote("alice", 20L, 0);
        verify(voteRepository).delete(existing);
        forumService.vote("alice", 20L, -1);
        assertThat(existing.getValue()).isEqualTo(-1);

        assertThatThrownBy(() -> forumService.vote("bob", 20L, 1)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void accept_onlyQuestionAuthor_togglesAndNotifies() {
        when(answerRepository.findByQuestionIdOrderByCreatedAtAsc(10L)).thenReturn(List.of(answer));

        assertThat(forumService.accept("alice", 10L, 20L).question().solved()).isTrue();
        verify(notificationService).notifyForumActivity(eq(bob), anyString(), anyString(), eq(10L));
        assertThat(forumService.accept("alice", 10L, 20L).question().solved()).isFalse();
        assertThatThrownBy(() -> forumService.accept("bob", 10L, 20L)).isInstanceOf(ForbiddenException.class);

        ForumQuestion another = new ForumQuestion();
        another.setId(11L);
        another.setAuthor(alice);
        when(questionRepository.findById(11L)).thenReturn(Optional.of(another));
        assertThatThrownBy(() -> forumService.accept("alice", 11L, 20L)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void deleteQuestionAndAnswer_requireAuthorOrModerator() {
        assertThatThrownBy(() -> forumService.deleteQuestion("bob", 10L)).isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> forumService.deleteAnswer("alice", 20L)).isInstanceOf(ForbiddenException.class);

        question.setAcceptedAnswerId(20L);
        forumService.deleteAnswer("bob", 20L);
        assertThat(question.getAcceptedAnswerId()).isNull();
        verify(voteRepository).deleteByAnswerId(20L);
        verify(answerRepository).delete(answer);

        bob.getRoles().add(new Role(3L, RoleName.ROLE_MANAGER));
        forumService.deleteQuestion("bob", 10L);
        verify(answerRepository).deleteByQuestionId(10L);
        verify(questionRepository).delete(question);

        assertThatThrownBy(() -> forumService.getThread("alice", 99L)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> forumService.vote("alice", 99L, 1)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> forumService.ask("ghost", new ForumQuestionRequestDTO()))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}

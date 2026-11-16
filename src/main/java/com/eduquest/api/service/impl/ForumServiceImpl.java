package com.eduquest.api.service.impl;

import com.eduquest.api.dto.request.ForumQuestionRequestDTO;
import com.eduquest.api.dto.response.ForumAnswerResponseDTO;
import com.eduquest.api.dto.response.ForumQuestionResponseDTO;
import com.eduquest.api.dto.response.ForumThreadResponseDTO;
import com.eduquest.api.dto.response.PageResponseDTO;
import com.eduquest.api.dto.response.UserResponseDTO;
import com.eduquest.api.dto.response.UserSummaryDTO;
import com.eduquest.api.entity.ForumAnswer;
import com.eduquest.api.entity.ForumQuestion;
import com.eduquest.api.entity.ForumVote;
import com.eduquest.api.entity.RoleName;
import com.eduquest.api.entity.User;
import com.eduquest.api.exception.BadRequestException;
import com.eduquest.api.exception.ForbiddenException;
import com.eduquest.api.exception.ResourceNotFoundException;
import com.eduquest.api.repository.ForumAnswerRepository;
import com.eduquest.api.repository.ForumQuestionRepository;
import com.eduquest.api.repository.ForumVoteRepository;
import com.eduquest.api.repository.UserRepository;
import com.eduquest.api.service.ForumService;
import com.eduquest.api.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class ForumServiceImpl implements ForumService {

    private static final Set<String> FILTERS = Set.of("all", "unanswered", "solved");

    private final ForumQuestionRepository questionRepository;
    private final ForumAnswerRepository answerRepository;
    private final ForumVoteRepository voteRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public ForumServiceImpl(ForumQuestionRepository questionRepository,
                            ForumAnswerRepository answerRepository,
                            ForumVoteRepository voteRepository,
                            UserRepository userRepository,
                            NotificationService notificationService) {
        this.questionRepository = questionRepository;
        this.answerRepository = answerRepository;
        this.voteRepository = voteRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<ForumQuestionResponseDTO> search(String username, String search, String course, String filter,
                                                            Pageable pageable) {
        User viewer = findUser(username);
        String cleanFilter = filter == null || filter.isBlank() ? "all" : filter.trim().toLowerCase(Locale.ROOT);
        if (!FILTERS.contains(cleanFilter)) {
            throw new BadRequestException("Filtro no válido: usa all, unanswered o solved");
        }
        String text = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        String pattern = "%" + text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        String cleanCourse = course == null ? "" : course.trim().toLowerCase(Locale.ROOT);

        Page<ForumQuestion> page = questionRepository.search(pattern, cleanCourse, cleanFilter, pageable);
        Map<Long, Long> counts = new HashMap<>();
        List<Long> ids = page.getContent().stream().map(ForumQuestion::getId).toList();
        if (!ids.isEmpty()) {
            for (Object[] row : answerRepository.countByQuestionIds(ids)) {
                counts.put((Long) row[0], ((Number) row[1]).longValue());
            }
        }
        List<ForumQuestionResponseDTO> content = page.getContent().stream()
                .map(question -> toResponse(question, counts.getOrDefault(question.getId(), 0L), viewer))
                .toList();
        return PageResponseDTO.of(content, page.getNumber(), page.getSize(), page.getTotalElements());
    }

    @Override
    @Transactional
    public ForumQuestionResponseDTO ask(String username, ForumQuestionRequestDTO request) {
        User author = findUser(username);
        ForumQuestion question = new ForumQuestion();
        question.setTitle(request.getTitle().trim());
        question.setBody(request.getBody().trim());
        question.setCourse(request.getCourse() == null || request.getCourse().isBlank() ? null : request.getCourse().trim());
        question.setAuthor(author);
        question.setCreatedAt(LocalDateTime.now());
        return toResponse(questionRepository.save(question), 0, author);
    }

    @Override
    @Transactional(readOnly = true)
    public ForumThreadResponseDTO getThread(String username, Long questionId) {
        return thread(findUser(username), findQuestion(questionId));
    }

    @Override
    @Transactional
    public void deleteQuestion(String username, Long questionId) {
        User user = findUser(username);
        ForumQuestion question = findQuestion(questionId);
        if (!canModerate(user, question.getAuthor())) {
            throw new ForbiddenException("Solo quien preguntó o un moderador puede eliminar la pregunta");
        }
        voteRepository.deleteByQuestionId(questionId);
        answerRepository.deleteByQuestionId(questionId);
        questionRepository.delete(question);
    }

    @Override
    @Transactional
    public ForumAnswerResponseDTO answer(String username, Long questionId, String body) {
        User author = findUser(username);
        ForumQuestion question = findQuestion(questionId);
        ForumAnswer answer = new ForumAnswer();
        answer.setQuestion(question);
        answer.setAuthor(author);
        answer.setBody(body.trim());
        answer.setCreatedAt(LocalDateTime.now());
        ForumAnswer saved = answerRepository.save(answer);
        if (!question.getAuthor().getId().equals(author.getId())) {
            notificationService.notifyForumActivity(question.getAuthor(),
                    "@" + author.getUsername() + " respondió tu pregunta", question.getTitle(), questionId);
        }
        return toResponse(saved, 0, 0, false, author);
    }

    @Override
    @Transactional
    public void deleteAnswer(String username, Long answerId) {
        User user = findUser(username);
        ForumAnswer answer = findAnswer(answerId);
        if (!canModerate(user, answer.getAuthor())) {
            throw new ForbiddenException("Solo el autor o un moderador puede eliminar la respuesta");
        }
        ForumQuestion question = answer.getQuestion();
        if (answerId.equals(question.getAcceptedAnswerId())) {
            question.setAcceptedAnswerId(null);
            questionRepository.save(question);
        }
        voteRepository.deleteByAnswerId(answerId);
        answerRepository.delete(answer);
    }

    @Override
    @Transactional
    public ForumAnswerResponseDTO vote(String username, Long answerId, int value) {
        User user = findUser(username);
        ForumAnswer answer = findAnswer(answerId);
        if (answer.getAuthor().getId().equals(user.getId())) {
            throw new BadRequestException("No puedes votar tu propia respuesta");
        }
        int normalized = Integer.signum(value);
        ForumVote vote = voteRepository.findByAnswerIdAndUserId(answerId, user.getId()).orElse(null);
        if (normalized == 0) {
            if (vote != null) {
                voteRepository.delete(vote);
            }
        } else {
            if (vote == null) {
                vote = new ForumVote();
                vote.setAnswer(answer);
                vote.setUser(user);
            }
            vote.setValue(normalized);
            voteRepository.save(vote);
        }
        voteRepository.flush();
        boolean accepted = answerId.equals(answer.getQuestion().getAcceptedAnswerId());
        return toResponse(answer, voteRepository.scoreOf(answerId), normalized, accepted, user);
    }

    @Override
    @Transactional
    public ForumThreadResponseDTO accept(String username, Long questionId, Long answerId) {
        User user = findUser(username);
        ForumQuestion question = findQuestion(questionId);
        if (!question.getAuthor().getId().equals(user.getId())) {
            throw new ForbiddenException("Solo quien preguntó puede marcar la mejor respuesta");
        }
        ForumAnswer answer = findAnswer(answerId);
        if (!answer.getQuestion().getId().equals(questionId)) {
            throw new BadRequestException("La respuesta no pertenece a esta pregunta");
        }
        boolean unmark = answerId.equals(question.getAcceptedAnswerId());
        question.setAcceptedAnswerId(unmark ? null : answerId);
        questionRepository.save(question);
        if (!unmark && !answer.getAuthor().getId().equals(user.getId())) {
            notificationService.notifyForumActivity(answer.getAuthor(),
                    "Tu respuesta fue elegida como la mejor", question.getTitle(), questionId);
        }
        return thread(user, question);
    }

    private ForumThreadResponseDTO thread(User viewer, ForumQuestion question) {
        Map<Long, Long> scores = new HashMap<>();
        for (Object[] row : voteRepository.sumByQuestion(question.getId())) {
            scores.put((Long) row[0], ((Number) row[1]).longValue());
        }
        Map<Long, Integer> myVotes = new HashMap<>();
        for (ForumVote vote : voteRepository.findByQuestionAndUser(question.getId(), viewer.getId())) {
            myVotes.put(vote.getAnswer().getId(), vote.getValue());
        }
        Long acceptedId = question.getAcceptedAnswerId();
        List<ForumAnswerResponseDTO> answers = answerRepository.findByQuestionIdOrderByCreatedAtAsc(question.getId()).stream()
                .map(answer -> toResponse(answer, scores.getOrDefault(answer.getId(), 0L),
                        myVotes.getOrDefault(answer.getId(), 0), answer.getId().equals(acceptedId), viewer))
                .sorted(Comparator.comparing(ForumAnswerResponseDTO::accepted).reversed()
                        .thenComparing(Comparator.comparingLong(ForumAnswerResponseDTO::score).reversed())
                        .thenComparing(ForumAnswerResponseDTO::createdAt))
                .toList();
        return new ForumThreadResponseDTO(toResponse(question, answers.size(), viewer), answers,
                question.getAuthor().getId().equals(viewer.getId()));
    }

    private boolean canModerate(User user, User author) {
        return author.getId().equals(user.getId())
                || user.hasRole(RoleName.ROLE_ADMIN) || user.hasRole(RoleName.ROLE_MANAGER);
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    private ForumQuestion findQuestion(Long id) {
        return questionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pregunta no encontrada"));
    }

    private ForumAnswer findAnswer(Long id) {
        return answerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Respuesta no encontrada"));
    }

    private static UserSummaryDTO summary(User user) {
        return new UserSummaryDTO(user.getId(), user.getUsername(),
                UserResponseDTO.avatarUrlFor(user.getId(), user.getAvatarVersion()), user.isAdvisor());
    }

    private ForumQuestionResponseDTO toResponse(ForumQuestion question, long answerCount, User viewer) {
        return new ForumQuestionResponseDTO(question.getId(), question.getTitle(), question.getBody(), question.getCourse(),
                summary(question.getAuthor()), question.getCreatedAt(), answerCount,
                question.getAcceptedAnswerId() != null, question.getAuthor().getId().equals(viewer.getId()),
                canModerate(viewer, question.getAuthor()));
    }

    private ForumAnswerResponseDTO toResponse(ForumAnswer answer, long score, int myVote, boolean accepted, User viewer) {
        return new ForumAnswerResponseDTO(answer.getId(), answer.getBody(), summary(answer.getAuthor()), answer.getCreatedAt(),
                score, myVote, accepted, answer.getAuthor().getId().equals(viewer.getId()),
                canModerate(viewer, answer.getAuthor()));
    }
}

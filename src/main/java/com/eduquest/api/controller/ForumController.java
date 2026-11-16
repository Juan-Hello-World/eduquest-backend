package com.eduquest.api.controller;

import com.eduquest.api.dto.request.ForumAnswerRequestDTO;
import com.eduquest.api.dto.request.ForumQuestionRequestDTO;
import com.eduquest.api.dto.request.ForumVoteRequestDTO;
import com.eduquest.api.dto.response.ForumAnswerResponseDTO;
import com.eduquest.api.dto.response.ForumQuestionResponseDTO;
import com.eduquest.api.dto.response.ForumThreadResponseDTO;
import com.eduquest.api.dto.response.PageResponseDTO;
import com.eduquest.api.service.ForumService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/forum")
@Tag(name = "Foro", description = "Preguntas por curso, respuestas, votos y mejor respuesta.")
public class ForumController {

    private final ForumService forumService;

    public ForumController(ForumService forumService) {
        this.forumService = forumService;
    }

    @GetMapping("/questions")
    @Operation(summary = "Buscar preguntas", description = "filter: all, unanswered o solved. Paginado, más recientes primero.")
    public ResponseEntity<PageResponseDTO<ForumQuestionResponseDTO>> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String course,
            @RequestParam(defaultValue = "all") String filter,
            @PageableDefault(size = 10) Pageable pageable,
            Authentication authentication) {
        return ResponseEntity.ok(forumService.search(authentication.getName(), search, course, filter, pageable));
    }

    @PostMapping("/questions")
    @Operation(summary = "Hacer una pregunta")
    public ResponseEntity<ForumQuestionResponseDTO> ask(@Valid @RequestBody ForumQuestionRequestDTO request,
                                                        Authentication authentication) {
        return new ResponseEntity<>(forumService.ask(authentication.getName(), request), HttpStatus.CREATED);
    }

    @GetMapping("/questions/{questionId}")
    @Operation(summary = "Ver pregunta con sus respuestas", description = "La mejor respuesta primero, luego por votos.")
    public ResponseEntity<ForumThreadResponseDTO> getThread(@PathVariable Long questionId, Authentication authentication) {
        return ResponseEntity.ok(forumService.getThread(authentication.getName(), questionId));
    }

    @DeleteMapping("/questions/{questionId}")
    @Operation(summary = "Eliminar pregunta", description = "Quien preguntó o un moderador.")
    public ResponseEntity<Void> deleteQuestion(@PathVariable Long questionId, Authentication authentication) {
        forumService.deleteQuestion(authentication.getName(), questionId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/questions/{questionId}/answers")
    @Operation(summary = "Responder")
    public ResponseEntity<ForumAnswerResponseDTO> answer(@PathVariable Long questionId,
                                                         @Valid @RequestBody ForumAnswerRequestDTO request,
                                                         Authentication authentication) {
        return new ResponseEntity<>(forumService.answer(authentication.getName(), questionId, request.getBody()),
                HttpStatus.CREATED);
    }

    @PostMapping("/questions/{questionId}/answers/{answerId}/accept")
    @Operation(summary = "Marcar o desmarcar la mejor respuesta", description = "Solo quien hizo la pregunta.")
    public ResponseEntity<ForumThreadResponseDTO> accept(@PathVariable Long questionId, @PathVariable Long answerId,
                                                         Authentication authentication) {
        return ResponseEntity.ok(forumService.accept(authentication.getName(), questionId, answerId));
    }

    @DeleteMapping("/answers/{answerId}")
    @Operation(summary = "Eliminar respuesta", description = "El autor o un moderador.")
    public ResponseEntity<Void> deleteAnswer(@PathVariable Long answerId, Authentication authentication) {
        forumService.deleteAnswer(authentication.getName(), answerId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/answers/{answerId}/vote")
    @Operation(summary = "Votar una respuesta", description = "value: 1 (útil), -1 (no útil) o 0 (quitar voto).")
    public ResponseEntity<ForumAnswerResponseDTO> vote(@PathVariable Long answerId,
                                                       @Valid @RequestBody ForumVoteRequestDTO request,
                                                       Authentication authentication) {
        return ResponseEntity.ok(forumService.vote(authentication.getName(), answerId, request.getValue()));
    }
}

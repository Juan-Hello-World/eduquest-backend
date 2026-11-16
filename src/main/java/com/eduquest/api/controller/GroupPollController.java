package com.eduquest.api.controller;

import com.eduquest.api.dto.request.PollRequestDTO;
import com.eduquest.api.dto.request.PollVoteRequestDTO;
import com.eduquest.api.dto.response.GroupMessageResponseDTO;
import com.eduquest.api.service.GroupMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/groups/{groupId}/polls")
@PreAuthorize("hasAuthority('ROLE_USER') or hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_MANAGER')")
@Tag(name = "Encuestas de grupo", description = "Encuestas rápidas dentro del chat del grupo.")
public class GroupPollController {

    private final GroupMessageService messageService;

    public GroupPollController(GroupMessageService messageService) {
        this.messageService = messageService;
    }

    @PostMapping
    @Operation(summary = "Crear encuesta", description = "Se publica como un mensaje en el canal indicado.")
    public ResponseEntity<GroupMessageResponseDTO> createPoll(@PathVariable Long groupId,
                                                              @Valid @RequestBody PollRequestDTO request,
                                                              Authentication authentication) {
        return new ResponseEntity<>(messageService.createPoll(groupId, request, authentication.getName()),
                HttpStatus.CREATED);
    }

    @PostMapping("/{pollId}/vote")
    @Operation(summary = "Votar", description = "Cambia tu voto; sin optionId lo retira.")
    public ResponseEntity<GroupMessageResponseDTO> vote(@PathVariable Long groupId,
                                                        @PathVariable Long pollId,
                                                        @RequestBody PollVoteRequestDTO request,
                                                        Authentication authentication) {
        return ResponseEntity.ok(messageService.votePoll(groupId, pollId, request.getOptionId(),
                authentication.getName()));
    }
}

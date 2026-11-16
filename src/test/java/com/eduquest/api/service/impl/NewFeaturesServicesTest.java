package com.eduquest.api.service.impl;

import com.eduquest.api.dto.request.GroupMessageRequestDTO;
import com.eduquest.api.dto.request.PinnedChatRequestDTO;
import com.eduquest.api.dto.request.RoomRequestDTO;
import com.eduquest.api.dto.request.RoomUpdateRequestDTO;
import com.eduquest.api.dto.request.StatusRequestDTO;
import com.eduquest.api.dto.request.StudyRoomRequestDTO;
import com.eduquest.api.dto.response.GlobalSearchDTO;
import com.eduquest.api.dto.response.RoomMessageResponseDTO;
import com.eduquest.api.dto.response.RoomResponseDTO;
import com.eduquest.api.dto.response.StoredFileDTO;
import com.eduquest.api.dto.response.StudyRoomDTO;
import com.eduquest.api.dto.response.StudySetResponseDTO;
import com.eduquest.api.dto.response.UserStatusDTO;
import com.eduquest.api.entity.Availability;
import com.eduquest.api.entity.ChatRoom;
import com.eduquest.api.entity.Document;
import com.eduquest.api.entity.ForumQuestion;
import com.eduquest.api.entity.PinnedChat;
import com.eduquest.api.entity.PinnedChatKind;
import com.eduquest.api.entity.PrivateGroup;
import com.eduquest.api.entity.RoomMessage;
import com.eduquest.api.entity.RoomVisit;
import com.eduquest.api.entity.StudySession;
import com.eduquest.api.entity.StudySet;
import com.eduquest.api.entity.StudySetType;
import com.eduquest.api.entity.User;
import com.eduquest.api.exception.BadRequestException;
import com.eduquest.api.exception.ExternalServiceException;
import com.eduquest.api.exception.ForbiddenException;
import com.eduquest.api.exception.InvalidOperationException;
import com.eduquest.api.exception.ResourceNotFoundException;
import com.eduquest.api.realtime.PresenceService;
import com.eduquest.api.repository.ChatRoomRepository;
import com.eduquest.api.repository.DocumentRepository;
import com.eduquest.api.repository.ForumQuestionRepository;
import com.eduquest.api.repository.PinnedChatRepository;
import com.eduquest.api.repository.PrivateGroupRepository;
import com.eduquest.api.repository.RoomMessageRepository;
import com.eduquest.api.repository.RoomVisitRepository;
import com.eduquest.api.repository.StudySessionRepository;
import com.eduquest.api.repository.StudySetRepository;
import com.eduquest.api.repository.UserRepository;
import com.eduquest.api.service.FileStorageService;
import com.eduquest.api.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NewFeaturesServicesTest {

    private UserRepository userRepository;
    private User alice;
    private User bob;
    private User carol;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        alice = user(1L, "alice");
        bob = user(2L, "bob");
        carol = user(3L, "carol");
        for (User user : List.of(alice, bob, carol)) {
            when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));
        }
    }

    private static User user(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        return user;
    }

    @Test
    void personalService_updatesStatusesAndPins() {
        PinnedChatRepository pins = mock(PinnedChatRepository.class);
        PresenceService presence = mock(PresenceService.class);
        PersonalServiceImpl service = new PersonalServiceImpl(userRepository, pins, presence);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(presence.isOnline(1L)).thenReturn(true);

        StatusRequestDTO request = new StatusRequestDTO();
        request.setAvailability(Availability.BUSY);
        request.setStatusText("  Estudiando Cálculo ");
        request.setStatusEmoji(" ");
        UserStatusDTO status = service.updateStatus("alice", request);
        assertThat(status.availability()).isEqualTo("BUSY");
        assertThat(status.statusText()).isEqualTo("Estudiando Cálculo");
        assertThat(status.statusEmoji()).isNull();
        assertThat(status.online()).isTrue();

        alice.setAvailability(Availability.INVISIBLE);
        when(userRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(alice, bob));
        List<UserStatusDTO> statuses = service.getStatuses(List.of(1L, 2L));
        assertThat(statuses.get(0).online()).isFalse();
        assertThat(statuses.get(0).availability()).isEqualTo("AVAILABLE");
        assertThatThrownBy(() -> service.getStatuses(java.util.stream.LongStream.rangeClosed(1, 101).boxed().toList()))
                .isInstanceOf(BadRequestException.class);

        PinnedChat pin = new PinnedChat();
        pin.setKind(PinnedChatKind.GROUP);
        pin.setTargetId(7L);
        when(pins.findByUserIdOrderByPinnedAtAsc(1L)).thenReturn(List.of(pin));
        PinnedChatRequestDTO pinRequest = new PinnedChatRequestDTO();
        pinRequest.setKind(PinnedChatKind.GROUP);
        pinRequest.setTargetId(7L);
        pinRequest.setPinned(true);
        assertThat(service.setPinned("alice", pinRequest)).extracting(item -> item.targetId()).containsExactly(7L);
        verify(pins).save(any(PinnedChat.class));

        when(pins.findByUserIdAndKindAndTargetId(1L, PinnedChatKind.GROUP, 7L)).thenReturn(Optional.of(pin));
        pinRequest.setPinned(false);
        service.setPinned("alice", pinRequest);
        verify(pins).delete(pin);

        pinRequest.setPinned(true);
        pinRequest.setTargetId(8L);
        when(pins.countByUserId(1L)).thenReturn((long) PersonalServiceImpl.MAX_PINNED);
        assertThatThrownBy(() -> service.setPinned("alice", pinRequest)).isInstanceOf(InvalidOperationException.class);
        assertThatThrownBy(() -> service.getPinnedChats("ghost")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void studyRoom_startJoinLeaveAndStop() {
        PrivateGroupRepository groups = mock(PrivateGroupRepository.class);
        ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
        StudyRoomServiceImpl service = new StudyRoomServiceImpl(groups, userRepository, events);
        PrivateGroup group = new PrivateGroup();
        group.setId(5L);
        group.setCreator(carol);
        group.getMembers().addAll(List.of(alice, bob, carol));
        when(groups.findById(5L)).thenReturn(Optional.of(group));
        when(groups.existsByIdAndMembersId(eq(5L), any())).thenReturn(true);

        assertThat(service.get(5L, "alice").active()).isFalse();
        assertThatThrownBy(() -> service.join(5L, "bob")).isInstanceOf(InvalidOperationException.class);

        StudyRoomRequestDTO request = new StudyRoomRequestDTO();
        request.setFocusMinutes(50);
        request.setBreakMinutes(10);
        StudyRoomDTO started = service.start(5L, request, "alice");
        assertThat(started.active()).isTrue();
        assertThat(started.focusMinutes()).isEqualTo(50);
        assertThatThrownBy(() -> service.start(5L, request, "bob")).isInstanceOf(InvalidOperationException.class);

        assertThat(service.join(5L, "bob").participants()).extracting(item -> item.username()).containsExactlyInAnyOrder("alice", "bob");
        assertThatThrownBy(() -> service.stop(5L, "bob")).isInstanceOf(ForbiddenException.class);
        assertThat(service.leave(5L, "alice").participants()).hasSize(1);
        assertThat(service.leave(5L, "bob").active()).isFalse();

        service.start(5L, request, "alice");
        assertThat(service.stop(5L, "carol").active()).isFalse();
        verify(events, org.mockito.Mockito.atLeastOnce()).publishEvent(any(com.eduquest.api.realtime.RealtimeEvent.class));

        when(groups.existsByIdAndMembersId(5L, 2L)).thenReturn(false);
        assertThatThrownBy(() -> service.get(5L, "bob")).isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> service.get(9L, "alice")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void chatRooms_fullLifecycle() {
        ChatRoomRepository rooms = mock(ChatRoomRepository.class);
        RoomMessageRepository messages = mock(RoomMessageRepository.class);
        RoomVisitRepository visits = mock(RoomVisitRepository.class);
        NotificationService notifications = mock(NotificationService.class);
        FileStorageService files = mock(FileStorageService.class);
        ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
        ChatRoomServiceImpl service = new ChatRoomServiceImpl(rooms, messages, visits, userRepository, notifications, files, events);
        when(userRepository.findAllById(any())).thenAnswer(invocation -> {
            List<User> found = new java.util.ArrayList<>();
            for (Object id : (Iterable<?>) invocation.getArgument(0)) {
                List.of(alice, bob, carol).stream().filter(user -> user.getId().equals(id)).forEach(found::add);
            }
            return found;
        });
        when(rooms.save(any(ChatRoom.class))).thenAnswer(invocation -> {
            ChatRoom room = invocation.getArgument(0);
            if (room.getId() == null) {
                room.setId(20L);
            }
            when(rooms.findById(20L)).thenReturn(Optional.of(room));
            return room;
        });
        java.util.concurrent.atomic.AtomicLong nextId = new java.util.concurrent.atomic.AtomicLong(100);
        Map<Long, RoomMessage> stored = new java.util.HashMap<>();
        when(messages.findById(any())).thenAnswer(invocation -> Optional.ofNullable(stored.get(invocation.<Long>getArgument(0))));
        when(messages.save(any(RoomMessage.class))).thenAnswer(invocation -> {
            RoomMessage message = invocation.getArgument(0);
            if (message.getId() == null) {
                message.setId(nextId.getAndIncrement());
            }
            stored.put(message.getId(), message);
            return message;
        });

        RoomRequestDTO request = new RoomRequestDTO();
        request.setName(" Repaso final ");
        request.setMemberIds(List.of(2L, 3L, 1L));
        RoomResponseDTO room = service.createRoom(request, "alice");
        assertThat(room.name()).isEqualTo("Repaso final");
        assertThat(room.members()).hasSize(3);

        request.setMemberIds(List.of(2L));
        assertThatThrownBy(() -> service.createRoom(request, "alice")).isInstanceOf(BadRequestException.class);
        request.setMemberIds(List.of(2L, 99L));
        assertThatThrownBy(() -> service.createRoom(request, "alice")).isInstanceOf(ResourceNotFoundException.class);

        GroupMessageRequestDTO text = new GroupMessageRequestDTO();
        text.setContent(" Hola equipo ");
        RoomMessageResponseDTO sent = service.sendMessage(20L, text, "alice");
        assertThat(sent.content()).isEqualTo("Hola equipo");
        assertThat(sent.mine()).isTrue();
        verify(notifications).notifyRoomMessage(eq(bob), eq(alice), eq(20L), eq("Repaso final"), anyString());

        GroupMessageRequestDTO reply = new GroupMessageRequestDTO();
        reply.setSticker("pack:bravo");
        reply.setReplyToId(100L);
        assertThat(service.sendMessage(20L, reply, "bob").replyTo().authorUsername()).isEqualTo("alice");

        assertThat(service.editMessage(20L, 100L, "Hola de nuevo", "alice").editedAt()).isNotNull();
        assertThatThrownBy(() -> service.editMessage(20L, 100L, "x", "bob")).isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> service.deleteMessage(20L, 100L, "bob")).isInstanceOf(ForbiddenException.class);
        assertThat(service.deleteMessage(20L, 100L, "alice").deleted()).isTrue();
        assertThatThrownBy(() -> service.editMessage(20L, 100L, "x", "alice")).isInstanceOf(BadRequestException.class);

        RoomMessage latest = new RoomMessage();
        latest.setId(101L);
        latest.setAuthor(bob);
        latest.setContent("Nos vemos");
        when(messages.findTopByRoomIdOrderByIdDesc(20L)).thenReturn(Optional.of(latest));
        when(messages.countByRoomIdAndAuthorIdNot(20L, 1L)).thenReturn(4L);
        ChatRoom created = rooms.findById(20L).orElseThrow();
        when(rooms.findByMember(1L)).thenReturn(List.of(created));
        RoomResponseDTO listed = service.getRooms("alice").getFirst();
        assertThat(listed.lastMessage()).isEqualTo("@bob: Nos vemos");
        assertThat(listed.unreadCount()).isEqualTo(4L);

        RoomVisit visit = new RoomVisit();
        visit.setLastReadAt(LocalDateTime.now());
        when(visits.findByUserIdAndRoomId(1L, 20L)).thenReturn(Optional.of(visit));
        service.markRead(20L, "alice");
        verify(notifications).markRoomRead(1L, 20L);
        assertThat(service.getRoom(20L, "alice").unreadCount()).isZero();

        when(messages.findTop50ByRoomIdOrderByIdDesc(20L)).thenReturn(List.of(latest));
        assertThat(service.getMessages(20L, "alice", null, null)).hasSize(1);
        service.getMessages(20L, "alice", 5L, null);
        service.getMessages(20L, "alice", null, 5L);

        RoomUpdateRequestDTO update = new RoomUpdateRequestDTO();
        update.setName("Final");
        assertThat(service.updateRoom(20L, update, "bob").name()).isEqualTo("Final");

        service.leaveRoom(20L, "carol");
        assertThatThrownBy(() -> service.getRoom(20L, "carol")).isInstanceOf(ForbiddenException.class);
        service.leaveRoom(20L, "bob");
        service.leaveRoom(20L, "alice");
        verify(rooms).delete(any(ChatRoom.class));
    }

    @Test
    void search_returnsEveryCategory() {
        DocumentRepository documents = mock(DocumentRepository.class);
        PrivateGroupRepository groups = mock(PrivateGroupRepository.class);
        ForumQuestionRepository questions = mock(ForumQuestionRepository.class);
        SearchServiceImpl service = new SearchServiceImpl(documents, groups, questions, userRepository);
        Document document = new Document();
        document.setId(1L);
        document.setTitle("Derivadas");
        document.setCourse("Cálculo");
        PrivateGroup group = new PrivateGroup();
        group.setId(2L);
        group.setName("Cálculo I");
        ForumQuestion question = new ForumQuestion();
        question.setId(3L);
        question.setTitle("¿Regla de la cadena?");
        when(documents.searchPublicDocuments(eq("%deri%"), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(document)));
        when(groups.searchForUser(eq("%deri%"), eq(1L), any(Pageable.class))).thenReturn(List.of(group));
        when(questions.search(eq("%deri%"), eq(""), eq("all"), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(question)));
        when(userRepository.searchActive(eq("%deri%"), eq(1L), any(Pageable.class))).thenReturn(List.of(bob));

        GlobalSearchDTO result = service.search(" @Deri ", "alice");

        assertThat(result.documents()).extracting(hit -> hit.subtitle()).containsExactly("Cálculo");
        assertThat(result.groups()).extracting(hit -> hit.title()).containsExactly("Cálculo I");
        assertThat(result.questions()).hasSize(1);
        assertThat(result.users()).extracting(hit -> hit.username()).containsExactly("bob");
        assertThat(service.search("a", "alice").documents()).isEmpty();
    }

    @Test
    void calendarExport_buildsIcsWithEscapedText() {
        StudySessionRepository sessions = mock(StudySessionRepository.class);
        CalendarExportServiceImpl service = new CalendarExportServiceImpl(sessions, userRepository);
        PrivateGroup group = new PrivateGroup();
        group.setName("Física, grupo A");
        StudySession session = new StudySession();
        session.setId(4L);
        session.setTitle("Repaso; parcial");
        session.setDescription("Traer dudas\nde cinemática");
        session.setMeetingUrl("https://meet.jit.si/EduQuest-sala-muy-larga-para-probar-que-las-lineas-se-pliegan-bien");
        session.setScheduledAt(LocalDateTime.of(2026, 11, 20, 18, 0));
        session.setPrivateGroup(group);
        when(sessions.findForMemberBetween(eq(1L), any(), any())).thenReturn(List.of(session));

        String ics = service.exportMySessions("alice");

        assertThat(ics).startsWith("BEGIN:VCALENDAR\r\n").endsWith("END:VCALENDAR\r\n");
        assertThat(ics).contains("UID:session-4@eduquest", "SUMMARY:Repaso\\; parcial · Física\\, grupo A", "TRIGGER:-PT30M");
        assertThat(ics.lines()).allMatch(line -> line.length() <= 75);
        assertThat(CalendarExportServiceImpl.escape("a\\b")).isEqualTo("a\\\\b");
    }

    @Test
    void geminiClient_parsesTextAndHandlesFailures() {
        RestTemplate restTemplate = mock(RestTemplate.class);
        GeminiClient client = new GeminiClient(restTemplate);
        Map<String, Object> body = Map.of("candidates", List.of(Map.of("content", Map.of("parts", List.of(
                Map.of("text", "pensando", "thought", true), Map.of("text", " Resumen listo "))))));
        org.mockito.Mockito.doReturn(ResponseEntity.ok(body), ResponseEntity.ok(Map.of("candidates", List.of())))
                .doThrow(new RestClientException("sin red"))
                .when(restTemplate).postForEntity(anyString(), any(), eq(Map.class));

        assertThat(client.generate("x", List.of(GeminiClient.text("hola")), 100, true)).contains("Resumen listo");
        assertThat(client.generate("x", List.of(GeminiClient.inlineData("text/plain", "aGk=")), 100, false)).isEmpty();
        assertThat(client.generate("x", List.of(), 100, false)).isEmpty();
    }

    @Test
    void studySets_generateCacheAndValidate() {
        DocumentRepository documents = mock(DocumentRepository.class);
        PrivateGroupRepository groups = mock(PrivateGroupRepository.class);
        StudySetRepository sets = mock(StudySetRepository.class);
        FileStorageService files = mock(FileStorageService.class);
        GeminiClient gemini = mock(GeminiClient.class);
        StudySetServiceImpl service = new StudySetServiceImpl(documents, groups, userRepository, sets, files, gemini,
                JsonMapper.builder().build());
        Document document = new Document();
        document.setId(7L);
        document.setTitle("Integrales");
        document.setDescription("Resumen del capítulo 5");
        document.setCourse("Cálculo II");
        document.setFileUrl("http://localhost:8080/api/v1/files/0f8c6a43-6a1e-4c55-9a6f-9a7d5a1b2c3d/integrales.pdf");
        when(documents.findById(7L)).thenReturn(Optional.of(document));
        when(files.loadByUrl(document.getFileUrl()))
                .thenReturn(Optional.of(new StoredFileDTO(new byte[]{1, 2}, "application/pdf", "integrales.pdf")));
        when(sets.save(any(StudySet.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(gemini.generate(anyString(), anyList(), anyInt(), anyBoolean()))
                .thenReturn(Optional.of("```json\n{\"questions\":[{\"question\":\"¿∫x dx?\",\"options\":[\"x²/2\",\"x\",\"1\",\"0\"],"
                        + "\"answerIndex\":0,\"explanation\":\"Regla de la potencia\"},{\"question\":\"\",\"options\":[],\"answerIndex\":0}]}\n```"))
                .thenReturn(Optional.of("{\"cards\":[{\"front\":\"Integral definida\",\"back\":\"Área bajo la curva\"}]}"))
                .thenReturn(Optional.of("{\"cards\":[]}"))
                .thenReturn(Optional.of("no es json"))
                .thenReturn(Optional.empty());

        StudySetResponseDTO quiz = service.getStudySet(7L, StudySetType.QUIZ, false, "alice");
        assertThat(quiz.questions()).hasSize(1);
        assertThat(quiz.questions().getFirst().answerIndex()).isZero();
        StudySetResponseDTO cards = service.getStudySet(7L, StudySetType.FLASHCARDS, false, "alice");
        assertThat(cards.cards()).extracting(StudySetResponseDTO.Card::front).containsExactly("Integral definida");
        assertThatThrownBy(() -> service.getStudySet(7L, StudySetType.FLASHCARDS, true, "alice"))
                .isInstanceOf(ExternalServiceException.class);
        assertThatThrownBy(() -> service.getStudySet(7L, StudySetType.QUIZ, true, "alice"))
                .isInstanceOf(ExternalServiceException.class);
        assertThatThrownBy(() -> service.getStudySet(7L, StudySetType.QUIZ, true, "alice"))
                .isInstanceOf(ExternalServiceException.class);

        StudySet cached = new StudySet();
        cached.setDocument(document);
        cached.setType(StudySetType.FLASHCARDS);
        cached.setContent("{\"cards\":[{\"front\":\"A\",\"back\":\"B\"}]}");
        when(sets.findByDocumentIdAndType(7L, StudySetType.FLASHCARDS)).thenReturn(Optional.of(cached));
        assertThat(service.getStudySet(7L, StudySetType.FLASHCARDS, false, "alice").cards()).hasSize(1);

        PrivateGroup group = new PrivateGroup();
        group.setId(3L);
        document.setGroup(group);
        assertThatThrownBy(() -> service.getStudySet(7L, StudySetType.QUIZ, false, "alice")).isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> service.getStudySet(8L, StudySetType.QUIZ, false, "alice"))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(files, never()).deleteByUrl(any());
    }
}

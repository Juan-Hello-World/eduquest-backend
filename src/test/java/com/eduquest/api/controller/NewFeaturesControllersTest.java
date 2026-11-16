package com.eduquest.api.controller;

import com.eduquest.api.dto.request.EditMessageRequestDTO;
import com.eduquest.api.dto.request.GroupMessageRequestDTO;
import com.eduquest.api.dto.request.ModerationRequestDTO;
import com.eduquest.api.dto.request.PinnedChatRequestDTO;
import com.eduquest.api.dto.request.RoomRequestDTO;
import com.eduquest.api.dto.request.RoomUpdateRequestDTO;
import com.eduquest.api.dto.request.StatusRequestDTO;
import com.eduquest.api.dto.request.StudyRoomRequestDTO;
import com.eduquest.api.dto.response.ChannelSummaryDTO;
import com.eduquest.api.dto.response.GlobalSearchDTO;
import com.eduquest.api.dto.response.GroupMessageResponseDTO;
import com.eduquest.api.dto.response.ModerationResponseDTO;
import com.eduquest.api.dto.response.RoomMessageResponseDTO;
import com.eduquest.api.dto.response.RoomResponseDTO;
import com.eduquest.api.dto.response.StudyRoomDTO;
import com.eduquest.api.dto.response.StudySetResponseDTO;
import com.eduquest.api.dto.response.UserStatusDTO;
import com.eduquest.api.dto.response.VoiceRoomDTO;
import com.eduquest.api.entity.StudySetType;
import com.eduquest.api.service.CalendarExportService;
import com.eduquest.api.service.ChatRoomService;
import com.eduquest.api.service.GroupCommunityService;
import com.eduquest.api.service.GroupMessageService;
import com.eduquest.api.service.PersonalService;
import com.eduquest.api.service.SearchService;
import com.eduquest.api.service.StudyRoomService;
import com.eduquest.api.service.StudySetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NewFeaturesControllersTest {

    private Authentication authentication;

    @BeforeEach
    void setUp() {
        authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("alice");
    }

    @Test
    void personalAndSearchAndCalendarControllers() {
        PersonalService personal = mock(PersonalService.class);
        PersonalController controller = new PersonalController(personal);
        UserStatusDTO status = new UserStatusDTO(1L, "BUSY", "Estudiando", null, true);
        StatusRequestDTO statusRequest = new StatusRequestDTO();
        PinnedChatRequestDTO pinRequest = new PinnedChatRequestDTO();
        when(personal.updateStatus("alice", statusRequest)).thenReturn(status);
        when(personal.getStatuses(List.of(1L))).thenReturn(List.of(status));
        when(personal.getPinnedChats("alice")).thenReturn(List.of());
        when(personal.setPinned("alice", pinRequest)).thenReturn(List.of());

        assertThat(controller.updateStatus(statusRequest, authentication).getBody()).isEqualTo(status);
        assertThat(controller.getStatuses(List.of(1L)).getBody()).containsExactly(status);
        assertThat(controller.getPinnedChats(authentication).getBody()).isEmpty();
        assertThat(controller.setPinned(pinRequest, authentication).getBody()).isEmpty();

        SearchService search = mock(SearchService.class);
        GlobalSearchDTO result = new GlobalSearchDTO(List.of(), List.of(), List.of(), List.of());
        when(search.search("calc", "alice")).thenReturn(result);
        assertThat(new SearchController(search).search("calc", authentication).getBody()).isSameAs(result);

        CalendarExportService calendar = mock(CalendarExportService.class);
        when(calendar.exportMySessions("alice")).thenReturn("BEGIN:VCALENDAR");
        var response = new CalendarController(calendar).export(authentication);
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION)).contains("eduquest-asesorias.ics");
        assertThat(new String(response.getBody())).isEqualTo("BEGIN:VCALENDAR");
    }

    @Test
    void groupChatExtrasControllers() {
        GroupCommunityService community = mock(GroupCommunityService.class);
        GroupCommunityController controller = new GroupCommunityController(community);
        ModerationRequestDTO moderation = new ModerationRequestDTO();
        moderation.setBannedWords(List.of("spam"));
        ModerationResponseDTO words = new ModerationResponseDTO(List.of("spam"));
        VoiceRoomDTO voice = new VoiceRoomDTO("https://meet.jit.si/x", List.of());
        when(community.getModeration(1L, "alice")).thenReturn(words);
        when(community.updateModeration(1L, List.of("spam"), "alice")).thenReturn(words);
        when(community.getVoice(1L, "alice")).thenReturn(voice);
        when(community.setInVoice(1L, true, "alice")).thenReturn(voice);
        when(community.setInVoice(1L, false, "alice")).thenReturn(voice);

        assertThat(controller.markChannelRead(1L, 4L, authentication).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(community).markChannelRead(1L, 4L, "alice");
        assertThat(controller.getModeration(1L, authentication).getBody()).isSameAs(words);
        assertThat(controller.updateModeration(1L, moderation, authentication).getBody()).isSameAs(words);
        assertThat(controller.getVoice(1L, authentication).getBody()).isSameAs(voice);
        assertThat(controller.joinVoice(1L, authentication).getBody()).isSameAs(voice);
        assertThat(controller.leaveVoice(1L, authentication).getBody()).isSameAs(voice);

        GroupMessageService messages = mock(GroupMessageService.class);
        GroupMessageController messageController = new GroupMessageController(messages);
        ChannelSummaryDTO summary = new ChannelSummaryDTO("Resumen", 3, true);
        GroupMessageResponseDTO saved = new GroupMessageResponseDTO();
        when(messages.summarize(1L, 4L, "alice")).thenReturn(summary);
        when(messages.setSaved(1L, 9L, true, "alice")).thenReturn(saved);
        when(messages.getSavedMessages("alice")).thenReturn(List.of());
        assertThat(messageController.summarize(1L, 4L, authentication).getBody()).isSameAs(summary);
        assertThat(messageController.setSaved(1L, 9L, true, authentication).getBody()).isSameAs(saved);
        assertThat(new SavedMessageController(messages).getSavedMessages(authentication).getBody()).isEmpty();

        StudyRoomService rooms = mock(StudyRoomService.class);
        StudyRoomController roomController = new StudyRoomController(rooms);
        StudyRoomDTO room = new StudyRoomDTO(true, 25, 5, LocalDateTime.now(), LocalDateTime.now(), 1L, List.of());
        StudyRoomRequestDTO roomRequest = new StudyRoomRequestDTO();
        when(rooms.get(1L, "alice")).thenReturn(room);
        when(rooms.start(1L, roomRequest, "alice")).thenReturn(room);
        when(rooms.join(1L, "alice")).thenReturn(room);
        when(rooms.leave(1L, "alice")).thenReturn(room);
        when(rooms.stop(1L, "alice")).thenReturn(room);
        assertThat(roomController.get(1L, authentication).getBody()).isSameAs(room);
        assertThat(roomController.start(1L, roomRequest, authentication).getBody()).isSameAs(room);
        assertThat(roomController.join(1L, authentication).getBody()).isSameAs(room);
        assertThat(roomController.leave(1L, authentication).getBody()).isSameAs(room);
        assertThat(roomController.stop(1L, authentication).getBody()).isSameAs(room);

        StudySetService sets = mock(StudySetService.class);
        StudySetResponseDTO set = new StudySetResponseDTO(7L, "QUIZ", List.of(), List.of(), true, LocalDateTime.now());
        when(sets.getStudySet(7L, StudySetType.QUIZ, false, "alice")).thenReturn(set);
        assertThat(new StudySetController(sets).getStudySet(7L, StudySetType.QUIZ, false, authentication).getBody()).isSameAs(set);
    }

    @Test
    void chatRoomController_delegates() {
        ChatRoomService service = mock(ChatRoomService.class);
        ChatRoomController controller = new ChatRoomController(service);
        RoomResponseDTO room = new RoomResponseDTO(2L, "Repaso", List.of(), 1L, null, LocalDateTime.now(), 0);
        RoomMessageResponseDTO message = new RoomMessageResponseDTO(5L, "Hola", null, null, null, null, 1L, "alice", null,
                true, LocalDateTime.now(), null, false, null);
        RoomRequestDTO create = new RoomRequestDTO();
        RoomUpdateRequestDTO update = new RoomUpdateRequestDTO();
        GroupMessageRequestDTO send = new GroupMessageRequestDTO();
        EditMessageRequestDTO edit = new EditMessageRequestDTO();
        edit.setContent("Hola!");
        when(service.getRooms("alice")).thenReturn(List.of(room));
        when(service.createRoom(create, "alice")).thenReturn(room);
        when(service.getRoom(2L, "alice")).thenReturn(room);
        when(service.updateRoom(2L, update, "alice")).thenReturn(room);
        when(service.getMessages(2L, "alice", null, null)).thenReturn(List.of(message));
        when(service.sendMessage(eq(2L), any(GroupMessageRequestDTO.class), eq("alice"))).thenReturn(message);
        when(service.editMessage(2L, 5L, "Hola!", "alice")).thenReturn(message);
        when(service.deleteMessage(2L, 5L, "alice")).thenReturn(message);

        assertThat(controller.getRooms(authentication).getBody()).containsExactly(room);
        assertThat(controller.createRoom(create, authentication).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(controller.getRoom(2L, authentication).getBody()).isSameAs(room);
        assertThat(controller.updateRoom(2L, update, authentication).getBody()).isSameAs(room);
        assertThat(controller.leaveRoom(2L, authentication).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(controller.getMessages(2L, null, null, authentication).getBody()).containsExactly(message);
        assertThat(controller.sendMessage(2L, send, authentication).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(controller.editMessage(2L, 5L, edit, authentication).getBody()).isSameAs(message);
        assertThat(controller.deleteMessage(2L, 5L, authentication).getBody()).isSameAs(message);
        assertThat(controller.markRead(2L, authentication).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(service).leaveRoom(2L, "alice");
        verify(service).markRead(2L, "alice");
    }
}

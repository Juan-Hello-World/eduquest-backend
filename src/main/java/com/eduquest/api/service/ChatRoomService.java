package com.eduquest.api.service;

import com.eduquest.api.dto.request.GroupMessageRequestDTO;
import com.eduquest.api.dto.request.RoomRequestDTO;
import com.eduquest.api.dto.request.RoomUpdateRequestDTO;
import com.eduquest.api.dto.response.RoomMessageResponseDTO;
import com.eduquest.api.dto.response.RoomResponseDTO;

import java.util.List;

public interface ChatRoomService {
    List<RoomResponseDTO> getRooms(String username);
    RoomResponseDTO getRoom(Long roomId, String username);
    RoomResponseDTO createRoom(RoomRequestDTO request, String username);
    RoomResponseDTO updateRoom(Long roomId, RoomUpdateRequestDTO request, String username);
    void leaveRoom(Long roomId, String username);
    List<RoomMessageResponseDTO> getMessages(Long roomId, String username, Long after, Long before);
    RoomMessageResponseDTO sendMessage(Long roomId, GroupMessageRequestDTO request, String username);
    RoomMessageResponseDTO editMessage(Long roomId, Long messageId, String content, String username);
    RoomMessageResponseDTO deleteMessage(Long roomId, Long messageId, String username);
    void markRead(Long roomId, String username);
}

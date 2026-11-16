package com.eduquest.api.service.impl;

import com.eduquest.api.dto.request.ChatMessages;
import com.eduquest.api.dto.request.GroupMessageRequestDTO;
import com.eduquest.api.dto.request.RoomRequestDTO;
import com.eduquest.api.dto.request.RoomUpdateRequestDTO;
import com.eduquest.api.dto.response.MessageReplyDTO;
import com.eduquest.api.dto.response.RoomMessageResponseDTO;
import com.eduquest.api.dto.response.RoomResponseDTO;
import com.eduquest.api.dto.response.UserResponseDTO;
import com.eduquest.api.dto.response.UserSummaryDTO;
import com.eduquest.api.entity.ChatRoom;
import com.eduquest.api.entity.RoomMessage;
import com.eduquest.api.entity.RoomVisit;
import com.eduquest.api.entity.User;
import com.eduquest.api.exception.BadRequestException;
import com.eduquest.api.exception.ForbiddenException;
import com.eduquest.api.exception.InvalidOperationException;
import com.eduquest.api.exception.ResourceNotFoundException;
import com.eduquest.api.realtime.RealtimeEvent;
import com.eduquest.api.repository.ChatRoomRepository;
import com.eduquest.api.repository.RoomMessageRepository;
import com.eduquest.api.repository.RoomVisitRepository;
import com.eduquest.api.repository.UserRepository;
import com.eduquest.api.service.ChatRoomService;
import com.eduquest.api.service.FileStorageService;
import com.eduquest.api.service.NotificationService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ChatRoomServiceImpl implements ChatRoomService {

    static final int MAX_MEMBERS = 10;
    private static final int PAGE_SIZE = 50;

    private final ChatRoomRepository roomRepository;
    private final RoomMessageRepository messageRepository;
    private final RoomVisitRepository visitRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final FileStorageService fileStorageService;
    private final ApplicationEventPublisher eventPublisher;

    public ChatRoomServiceImpl(ChatRoomRepository roomRepository,
                               RoomMessageRepository messageRepository,
                               RoomVisitRepository visitRepository,
                               UserRepository userRepository,
                               NotificationService notificationService,
                               FileStorageService fileStorageService,
                               ApplicationEventPublisher eventPublisher) {
        this.roomRepository = roomRepository;
        this.messageRepository = messageRepository;
        this.visitRepository = visitRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.fileStorageService = fileStorageService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomResponseDTO> getRooms(String username) {
        User me = findUser(username);
        return roomRepository.findByMember(me.getId()).stream().map(room -> toResponse(room, me)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RoomResponseDTO getRoom(Long roomId, String username) {
        User me = findUser(username);
        return toResponse(findRoom(roomId, me), me);
    }

    @Override
    @Transactional
    public RoomResponseDTO createRoom(RoomRequestDTO request, String username) {
        User me = findUser(username);
        Set<User> members = new LinkedHashSet<>();
        members.add(me);
        members.addAll(resolveUsers(request.getMemberIds(), me));
        if (members.size() < 3) {
            throw new BadRequestException("Un chat grupal necesita al menos 3 personas contándote");
        }
        if (members.size() > MAX_MEMBERS) {
            throw new InvalidOperationException("Un chat grupal tiene hasta " + MAX_MEMBERS + " personas");
        }
        ChatRoom room = new ChatRoom();
        room.setName(request.getName().trim());
        room.setCreatedBy(me);
        room.getMembers().addAll(members);
        LocalDateTime now = LocalDateTime.now();
        room.setCreatedAt(now);
        room.setLastActivityAt(now);
        ChatRoom saved = roomRepository.save(room);
        publishRoomUpdate(saved);
        return toResponse(saved, me);
    }

    @Override
    @Transactional
    public RoomResponseDTO updateRoom(Long roomId, RoomUpdateRequestDTO request, String username) {
        User me = findUser(username);
        ChatRoom room = findRoom(roomId, me);
        if (request.getName() != null && !request.getName().isBlank()) {
            room.setName(request.getName().trim());
        }
        if (request.getAddMemberIds() != null && !request.getAddMemberIds().isEmpty()) {
            List<User> added = resolveUsers(request.getAddMemberIds(), me);
            room.getMembers().addAll(added);
            if (room.getMembers().size() > MAX_MEMBERS) {
                throw new InvalidOperationException("Un chat grupal tiene hasta " + MAX_MEMBERS + " personas");
            }
        }
        ChatRoom saved = roomRepository.save(room);
        publishRoomUpdate(saved);
        return toResponse(saved, me);
    }

    @Override
    @Transactional
    public void leaveRoom(Long roomId, String username) {
        User me = findUser(username);
        ChatRoom room = findRoom(roomId, me);
        room.getMembers().removeIf(member -> member.getId().equals(me.getId()));
        visitRepository.deleteByRoomIdAndUserId(roomId, me.getId());
        if (room.getMembers().isEmpty()) {
            messageRepository.clearRepliesByRoomId(roomId);
            messageRepository.deleteByRoomId(roomId);
            visitRepository.deleteByRoomId(roomId);
            roomRepository.delete(room);
            return;
        }
        publishRoomUpdate(roomRepository.save(room));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomMessageResponseDTO> getMessages(Long roomId, String username, Long after, Long before) {
        User me = findUser(username);
        findRoom(roomId, me);
        List<RoomMessage> messages;
        if (after != null) {
            messages = messageRepository.findTop100ByRoomIdAndIdGreaterThanOrderByIdAsc(roomId, after);
        } else {
            messages = new ArrayList<>(before != null
                    ? messageRepository.findTop50ByRoomIdAndIdLessThanOrderByIdDesc(roomId, before)
                    : messageRepository.findTop50ByRoomIdOrderByIdDesc(roomId));
            Collections.reverse(messages);
        }
        return messages.stream().map(message -> toMessage(message, me)).toList();
    }

    @Override
    @Transactional
    public RoomMessageResponseDTO sendMessage(Long roomId, GroupMessageRequestDTO request, String username) {
        User me = findUser(username);
        ChatRoom room = findRoom(roomId, me);
        boolean hasAttachment = ChatMessages.present(request.getImageUrl()) || ChatMessages.present(request.getFileUrl())
                || ChatMessages.present(request.getSticker());
        RoomMessage message = new RoomMessage();
        message.setRoom(room);
        message.setAuthor(me);
        message.setContent(ChatMessages.cleanContent(request.getContent(), hasAttachment));
        message.setImageUrl(ChatMessages.cleanImage(request.getImageUrl()));
        message.setFileUrl(ChatMessages.present(request.getFileUrl()) ? request.getFileUrl().trim() : null);
        message.setFileName(ChatMessages.cleanFileName(request.getFileName(), message.getFileUrl()));
        message.setSticker(ChatMessages.present(request.getSticker()) ? request.getSticker().trim() : null);
        message.setCreatedAt(LocalDateTime.now());
        if (request.getReplyToId() != null) {
            message.setReplyTo(findMessage(roomId, request.getReplyToId()));
        }
        RoomMessage saved = messageRepository.save(message);
        room.setLastActivityAt(saved.getCreatedAt());
        roomRepository.save(room);
        broadcast(room, "created", saved);
        String preview = ChatMessages.preview(saved.getContent(), saved.getImageUrl(), saved.getFileName(), saved.getSticker());
        room.getMembers().stream()
                .filter(member -> !member.getId().equals(me.getId()))
                .forEach(member -> notificationService.notifyRoomMessage(member, me, roomId, room.getName(), preview));
        return toMessage(saved, me);
    }

    @Override
    @Transactional
    public RoomMessageResponseDTO editMessage(Long roomId, Long messageId, String content, String username) {
        User me = findUser(username);
        ChatRoom room = findRoom(roomId, me);
        RoomMessage message = findMessage(roomId, messageId);
        if (!message.getAuthor().getId().equals(me.getId())) {
            throw new ForbiddenException("Solo puedes editar tus propios mensajes");
        }
        if (message.isDeleted()) {
            throw new BadRequestException("No puedes editar un mensaje eliminado");
        }
        boolean hasAttachment = message.getImageUrl() != null || message.getFileUrl() != null || message.getSticker() != null;
        message.setContent(ChatMessages.cleanContent(content, hasAttachment));
        message.setEditedAt(LocalDateTime.now());
        RoomMessage saved = messageRepository.save(message);
        broadcast(room, "updated", saved);
        return toMessage(saved, me);
    }

    @Override
    @Transactional
    public RoomMessageResponseDTO deleteMessage(Long roomId, Long messageId, String username) {
        User me = findUser(username);
        ChatRoom room = findRoom(roomId, me);
        RoomMessage message = findMessage(roomId, messageId);
        if (!message.getAuthor().getId().equals(me.getId())) {
            throw new ForbiddenException("Solo puedes eliminar tus propios mensajes");
        }
        if (!message.isDeleted()) {
            List<String> files = new ArrayList<>();
            if (message.getImageUrl() != null) {
                files.add(message.getImageUrl());
            }
            if (message.getFileUrl() != null) {
                files.add(message.getFileUrl());
            }
            message.setDeleted(true);
            message.setContent("");
            message.setImageUrl(null);
            message.setFileUrl(null);
            message.setFileName(null);
            message.setSticker(null);
            message = messageRepository.save(message);
            files.forEach(fileStorageService::deleteByUrl);
            broadcast(room, "deleted", message);
        }
        return toMessage(message, me);
    }

    @Override
    @Transactional
    public void markRead(Long roomId, String username) {
        User me = findUser(username);
        ChatRoom room = findRoom(roomId, me);
        RoomVisit visit = visitRepository.findByUserIdAndRoomId(me.getId(), roomId).orElseGet(() -> {
            RoomVisit created = new RoomVisit();
            created.setUser(me);
            created.setRoom(room);
            return created;
        });
        visit.setLastReadAt(LocalDateTime.now());
        visitRepository.save(visit);
        notificationService.markRoomRead(me.getId(), roomId);
    }

    private List<User> resolveUsers(Collection<Long> ids, User me) {
        Set<Long> unique = new LinkedHashSet<>(ids);
        unique.remove(me.getId());
        List<User> users = userRepository.findAllById(unique);
        if (users.size() != unique.size()) {
            throw new ResourceNotFoundException("Alguna de las personas elegidas no existe");
        }
        if (users.stream().anyMatch(User::isBanned)) {
            throw new InvalidOperationException("No puedes agregar cuentas suspendidas");
        }
        return users;
    }

    private void publishRoomUpdate(ChatRoom room) {
        eventPublisher.publishEvent(new RealtimeEvent(this, memberIds(room), Map.of("type", "room.updated", "roomId", room.getId())));
    }

    private void broadcast(ChatRoom room, String action, RoomMessage message) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "room.message");
        payload.put("action", action);
        payload.put("roomId", room.getId());
        payload.put("message", toMessage(message, null));
        eventPublisher.publishEvent(new RealtimeEvent(this, memberIds(room), payload));
    }

    private static List<Long> memberIds(ChatRoom room) {
        return room.getMembers().stream().map(User::getId).toList();
    }

    private RoomResponseDTO toResponse(ChatRoom room, User viewer) {
        List<UserSummaryDTO> members = room.getMembers().stream()
                .sorted(Comparator.comparing(User::getUsername, String.CASE_INSENSITIVE_ORDER))
                .map(ChatRoomServiceImpl::summary)
                .toList();
        String last = messageRepository.findTopByRoomIdOrderByIdDesc(room.getId())
                .map(message -> message.isDeleted() ? ChatMessages.DELETED_PREVIEW
                        : "@" + message.getAuthor().getUsername() + ": " + ChatMessages.preview(message.getContent(),
                        message.getImageUrl(), message.getFileName(), message.getSticker()))
                .orElse(null);
        long unread = visitRepository.findByUserIdAndRoomId(viewer.getId(), room.getId())
                .map(visit -> messageRepository.countByRoomIdAndCreatedAtAfterAndAuthorIdNot(room.getId(), visit.getLastReadAt(), viewer.getId()))
                .orElseGet(() -> messageRepository.countByRoomIdAndAuthorIdNot(room.getId(), viewer.getId()));
        return new RoomResponseDTO(room.getId(), room.getName(), members, room.getCreatedBy().getId(), last,
                room.getLastActivityAt(), unread);
    }

    private static RoomMessageResponseDTO toMessage(RoomMessage message, User viewer) {
        boolean deleted = message.isDeleted();
        RoomMessage reply = message.getReplyTo();
        MessageReplyDTO replyTo = reply == null ? null : new MessageReplyDTO(reply.getId(), reply.getAuthor().getId(),
                reply.getAuthor().getUsername(), ChatMessages.replyPreview(reply.getContent(), reply.getImageUrl(),
                reply.getFileName(), reply.getSticker(), reply.isDeleted()), reply.isDeleted());
        User author = message.getAuthor();
        return new RoomMessageResponseDTO(message.getId(), deleted ? "" : message.getContent(),
                deleted ? null : message.getImageUrl(), deleted ? null : message.getFileUrl(),
                deleted ? null : message.getFileName(), deleted ? null : message.getSticker(),
                author.getId(), author.getUsername(), UserResponseDTO.avatarUrlFor(author.getId(), author.getAvatarVersion()),
                viewer != null && author.getId().equals(viewer.getId()), message.getCreatedAt(), message.getEditedAt(),
                deleted, replyTo);
    }

    private static UserSummaryDTO summary(User user) {
        return new UserSummaryDTO(user.getId(), user.getUsername(),
                UserResponseDTO.avatarUrlFor(user.getId(), user.getAvatarVersion()), user.isAdvisor());
    }

    private ChatRoom findRoom(Long roomId, User user) {
        ChatRoom room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat no encontrado"));
        if (!room.hasMember(user.getId())) {
            throw new ForbiddenException("No formas parte de este chat");
        }
        return room;
    }

    private RoomMessage findMessage(Long roomId, Long messageId) {
        return messageRepository.findById(messageId)
                .filter(message -> message.getRoom().getId().equals(roomId))
                .orElseThrow(() -> new ResourceNotFoundException("Mensaje no encontrado"));
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }
}

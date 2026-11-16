package com.eduquest.api.service.impl;

import com.eduquest.api.dto.request.ChannelRequestDTO;
import com.eduquest.api.dto.response.ChannelResponseDTO;
import com.eduquest.api.dto.response.ModerationResponseDTO;
import com.eduquest.api.dto.response.UserResponseDTO;
import com.eduquest.api.dto.response.UserSummaryDTO;
import com.eduquest.api.dto.response.VoiceRoomDTO;
import com.eduquest.api.entity.Availability;
import com.eduquest.api.entity.ChannelRead;
import com.eduquest.api.entity.GroupChannel;
import com.eduquest.api.entity.GroupVisit;
import com.eduquest.api.entity.PrivateGroup;
import com.eduquest.api.entity.RoleName;
import com.eduquest.api.entity.User;
import com.eduquest.api.exception.BadRequestException;
import com.eduquest.api.exception.DuplicateResourceException;
import com.eduquest.api.exception.ForbiddenException;
import com.eduquest.api.exception.InvalidOperationException;
import com.eduquest.api.exception.ResourceNotFoundException;
import com.eduquest.api.realtime.PresenceService;
import com.eduquest.api.realtime.RealtimeEvent;
import com.eduquest.api.repository.ChannelReadRepository;
import com.eduquest.api.repository.GroupChannelRepository;
import com.eduquest.api.repository.GroupMessageRepository;
import com.eduquest.api.repository.GroupVisitRepository;
import com.eduquest.api.repository.MessageReactionRepository;
import com.eduquest.api.repository.PollRepository;
import com.eduquest.api.repository.PrivateGroupRepository;
import com.eduquest.api.repository.UserRepository;
import com.eduquest.api.service.GroupCommunityService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.text.Normalizer;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GroupCommunityServiceImpl implements GroupCommunityService {

    static final String DEFAULT_CHANNEL = "general";
    static final int MAX_CHANNELS = 20;
    private static final String VOICE_BASE_URL = "https://meet.jit.si/";
    private static final String ROOM_ALPHABET = "abcdefghijkmnpqrstuvwxyz23456789";

    private final PrivateGroupRepository groupRepository;
    private final GroupChannelRepository channelRepository;
    private final GroupMessageRepository messageRepository;
    private final MessageReactionRepository reactionRepository;
    private final PollRepository pollRepository;
    private final GroupVisitRepository visitRepository;
    private final UserRepository userRepository;
    private final PresenceService presenceService;
    private final ApplicationEventPublisher eventPublisher;
    private final ChannelReadRepository channelReadRepository;
    private final SecureRandom random = new SecureRandom();
    private final Map<Long, Map<Long, LocalDateTime>> voiceParticipants = new ConcurrentHashMap<>();

    static final int MAX_BANNED_WORDS = 50;
    static final Duration VOICE_TTL = Duration.ofHours(3);

    public GroupCommunityServiceImpl(PrivateGroupRepository groupRepository,
                                     GroupChannelRepository channelRepository,
                                     GroupMessageRepository messageRepository,
                                     MessageReactionRepository reactionRepository,
                                     PollRepository pollRepository,
                                     GroupVisitRepository visitRepository,
                                     UserRepository userRepository,
                                     PresenceService presenceService,
                                     ApplicationEventPublisher eventPublisher,
                                     ChannelReadRepository channelReadRepository) {
        this.groupRepository = groupRepository;
        this.channelRepository = channelRepository;
        this.messageRepository = messageRepository;
        this.reactionRepository = reactionRepository;
        this.pollRepository = pollRepository;
        this.visitRepository = visitRepository;
        this.userRepository = userRepository;
        this.presenceService = presenceService;
        this.eventPublisher = eventPublisher;
        this.channelReadRepository = channelReadRepository;
    }

    @Override
    @Transactional
    public List<ChannelResponseDTO> getChannels(Long groupId, String username) {
        PrivateGroup group = findGroup(groupId);
        User user = findUser(username);
        requireMember(group, user);
        ensureDefaultChannel(group);
        Map<Long, Long> unread = toCounts(messageRepository.countUnreadByChannel(groupId, user.getId()));
        Map<Long, Long> mentions = toCounts(messageRepository.countMentionsByChannel(groupId, user.getId(),
                "%@" + likeEscape(user.getUsername().toLowerCase(Locale.ROOT)) + "%"));
        return channelRepository.findByGroupIdOrderByPositionAscIdAsc(groupId).stream()
                .map(channel -> toResponse(channel, unread.getOrDefault(channel.getId(), 0L),
                        mentions.getOrDefault(channel.getId(), 0L)))
                .toList();
    }

    @Override
    @Transactional
    public ChannelResponseDTO createChannel(Long groupId, ChannelRequestDTO request, String username) {
        PrivateGroup group = findGroup(groupId);
        requireManager(group, findUser(username));
        ensureDefaultChannel(group);
        List<GroupChannel> channels = channelRepository.findByGroupIdOrderByPositionAscIdAsc(groupId);
        if (channels.size() >= MAX_CHANNELS) {
            throw new InvalidOperationException("Un grupo puede tener hasta " + MAX_CHANNELS + " canales");
        }
        String name = normalizeName(request.getName());
        ensureUniqueName(channels, name, null);
        GroupChannel channel = new GroupChannel();
        channel.setGroup(group);
        channel.setName(name);
        channel.setTopic(cleanTopic(request.getTopic()));
        applySettings(channel, request);
        channel.setPosition(channels.stream().mapToInt(GroupChannel::getPosition).max().orElse(0) + 1);
        channel.setCreatedAt(LocalDateTime.now());
        GroupChannel saved = channelRepository.save(channel);
        broadcastChannels(group);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public ChannelResponseDTO updateChannel(Long groupId, Long channelId, ChannelRequestDTO request, String username) {
        PrivateGroup group = findGroup(groupId);
        requireManager(group, findUser(username));
        GroupChannel channel = findChannel(groupId, channelId);
        String name = normalizeName(request.getName());
        ensureUniqueName(channelRepository.findByGroupIdOrderByPositionAscIdAsc(groupId), name, channelId);
        channel.setName(name);
        channel.setTopic(cleanTopic(request.getTopic()));
        applySettings(channel, request);
        GroupChannel saved = channelRepository.save(channel);
        broadcastChannels(group);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteChannel(Long groupId, Long channelId, String username) {
        PrivateGroup group = findGroup(groupId);
        requireManager(group, findUser(username));
        GroupChannel channel = findChannel(groupId, channelId);
        if (channelRepository.countByGroupId(groupId) <= 1) {
            throw new InvalidOperationException("El grupo necesita al menos un canal");
        }
        List<Long> pollIds = messageRepository.findPollIdsByChannelId(channelId);
        reactionRepository.deleteByChannelId(channelId);
        messageRepository.clearRepliesToChannel(channelId);
        messageRepository.clearLinksByChannelId(channelId);
        messageRepository.deleteByChannelId(channelId);
        if (!pollIds.isEmpty()) {
            pollRepository.deleteVotesByPollIds(pollIds);
            pollRepository.deleteOptionsByPollIds(pollIds);
            pollRepository.deleteByIds(pollIds);
        }
        channelRepository.delete(channel);
        broadcastChannels(group);
    }

    @Override
    @Transactional
    public void setModerator(Long groupId, Long memberId, boolean moderator, String username) {
        PrivateGroup group = findGroup(groupId);
        User user = findUser(username);
        if (!group.isCreator(user) && !user.hasRole(RoleName.ROLE_ADMIN)) {
            throw new ForbiddenException("Solo el creador del grupo puede nombrar moderadores");
        }
        User member = group.getMembers().stream()
                .filter(candidate -> candidate.getId().equals(memberId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Esa persona no es miembro del grupo"));
        if (group.isCreator(member)) {
            throw new InvalidOperationException("El creador ya administra el grupo");
        }
        if (moderator) {
            group.getModerators().add(member);
        } else {
            group.getModerators().removeIf(current -> current.getId().equals(memberId));
        }
        groupRepository.save(group);
    }

    @Override
    @Transactional
    public void setMuted(Long groupId, boolean muted, String username) {
        PrivateGroup group = findGroup(groupId);
        User user = findUser(username);
        requireMember(group, user);
        GroupVisit visit = visitRepository.findByUserIdAndGroupId(user.getId(), groupId)
                .orElseGet(() -> new GroupVisit(user, group, LocalDateTime.now()));
        visit.setMuted(muted);
        visitRepository.save(visit);
    }

    @Override
    @Transactional
    public String getVoiceRoomUrl(Long groupId, String username) {
        PrivateGroup group = findGroup(groupId);
        requireMember(group, findUser(username));
        if (group.getVoiceRoom() == null) {
            StringBuilder room = new StringBuilder("EduQuest-").append(slug(group.getName())).append('-');
            for (int index = 0; index < 12; index++) {
                room.append(ROOM_ALPHABET.charAt(random.nextInt(ROOM_ALPHABET.length())));
            }
            group.setVoiceRoom(room.toString());
            groupRepository.save(group);
        }
        return VOICE_BASE_URL + group.getVoiceRoom();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> getOnlineMembers(Long groupId, String username) {
        PrivateGroup group = findGroup(groupId);
        requireMember(group, findUser(username));
        return group.getMembers().stream()
                .filter(member -> member.getAvailability() != Availability.INVISIBLE)
                .map(User::getId)
                .filter(presenceService::isOnline)
                .sorted()
                .toList();
    }

    @Override
    @Transactional
    public GroupChannel resolveChannel(PrivateGroup group, Long channelId) {
        if (channelId == null) {
            return ensureDefaultChannel(group);
        }
        return findChannel(group.getId(), channelId);
    }

    @Override
    @Transactional
    public void markChannelRead(Long groupId, Long channelId, String username) {
        PrivateGroup group = findGroup(groupId);
        User user = findUser(username);
        requireMember(group, user);
        GroupChannel channel = findChannel(groupId, channelId);
        Long lastId = messageRepository.findTopByChannelIdOrderByIdDesc(channelId).map(message -> message.getId()).orElse(0L);
        ChannelRead read = channelReadRepository.findByUserIdAndChannelId(user.getId(), channelId).orElseGet(() -> {
            ChannelRead created = new ChannelRead();
            created.setUser(user);
            created.setChannel(channel);
            return created;
        });
        read.setLastReadMessageId(lastId);
        read.setUpdatedAt(LocalDateTime.now());
        channelReadRepository.save(read);
    }

    @Override
    @Transactional(readOnly = true)
    public ModerationResponseDTO getModeration(Long groupId, String username) {
        PrivateGroup group = findGroup(groupId);
        requireMember(group, findUser(username));
        return new ModerationResponseDTO(bannedWordsOf(group));
    }

    @Override
    @Transactional
    public ModerationResponseDTO updateModeration(Long groupId, List<String> bannedWords, String username) {
        PrivateGroup group = findGroup(groupId);
        requireManager(group, findUser(username));
        LinkedHashSet<String> words = new LinkedHashSet<>();
        for (String word : bannedWords) {
            String clean = normalizeWord(word);
            if (!clean.isEmpty()) {
                words.add(clean);
            }
        }
        if (words.size() > MAX_BANNED_WORDS) {
            throw new BadRequestException("Puedes bloquear hasta " + MAX_BANNED_WORDS + " palabras");
        }
        group.setBannedWords(words.isEmpty() ? null : String.join("\n", words));
        groupRepository.save(group);
        return new ModerationResponseDTO(List.copyOf(words));
    }

    @Override
    @Transactional(readOnly = true)
    public VoiceRoomDTO getVoice(Long groupId, String username) {
        PrivateGroup group = findGroup(groupId);
        requireMember(group, findUser(username));
        return new VoiceRoomDTO(group.getVoiceRoom() == null ? null : VOICE_BASE_URL + group.getVoiceRoom(),
                voiceMembers(group));
    }

    @Override
    @Transactional
    public VoiceRoomDTO setInVoice(Long groupId, boolean inVoice, String username) {
        PrivateGroup group = findGroup(groupId);
        User user = findUser(username);
        requireMember(group, user);
        String url = getVoiceRoomUrl(groupId, username);
        Map<Long, LocalDateTime> participants = voiceParticipants.computeIfAbsent(groupId, id -> new ConcurrentHashMap<>());
        if (inVoice) {
            participants.put(user.getId(), LocalDateTime.now());
        } else {
            participants.remove(user.getId());
        }
        List<UserSummaryDTO> members = voiceMembers(group);
        eventPublisher.publishEvent(new RealtimeEvent(this, group.getMembers().stream().map(User::getId).toList(),
                Map.of("type", "group.voice", "groupId", groupId, "participants", members)));
        return new VoiceRoomDTO(url, members);
    }

    public static List<String> bannedWordsOf(PrivateGroup group) {
        String words = group.getBannedWords();
        return words == null || words.isBlank() ? List.of() : List.of(words.split("\n"));
    }

    public static String normalizeWord(String raw) {
        return Normalizer.normalize(raw == null ? "" : raw, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    private List<UserSummaryDTO> voiceMembers(PrivateGroup group) {
        Map<Long, LocalDateTime> participants = voiceParticipants.getOrDefault(group.getId(), Map.of());
        LocalDateTime limit = LocalDateTime.now().minus(VOICE_TTL);
        participants.entrySet().removeIf(entry -> entry.getValue().isBefore(limit) || !presenceService.isOnline(entry.getKey()));
        return group.getMembers().stream()
                .filter(member -> participants.containsKey(member.getId()))
                .sorted(Comparator.comparing(member -> participants.get(member.getId())))
                .map(member -> new UserSummaryDTO(member.getId(), member.getUsername(),
                        UserResponseDTO.avatarUrlFor(member.getId(), member.getAvatarVersion()), member.isAdvisor()))
                .toList();
    }

    private static void applySettings(GroupChannel channel, ChannelRequestDTO request) {
        if (request.getReadOnly() != null) {
            channel.setReadOnly(request.getReadOnly());
        }
        if (request.getSlowModeSeconds() != null) {
            channel.setSlowModeSeconds(request.getSlowModeSeconds());
        }
    }

    private static Map<Long, Long> toCounts(List<Object[]> rows) {
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : rows) {
            counts.put((Long) row[0], ((Number) row[1]).longValue());
        }
        return counts;
    }

    static String likeEscape(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private GroupChannel ensureDefaultChannel(PrivateGroup group) {
        List<GroupChannel> channels = channelRepository.findByGroupIdOrderByPositionAscIdAsc(group.getId());
        if (!channels.isEmpty()) {
            return channels.getFirst();
        }
        GroupChannel channel = new GroupChannel();
        channel.setGroup(group);
        channel.setName(DEFAULT_CHANNEL);
        channel.setTopic("Conversación general del grupo");
        channel.setPosition(0);
        channel.setCreatedAt(LocalDateTime.now());
        GroupChannel saved = channelRepository.save(channel);
        messageRepository.assignChannel(group.getId(), saved);
        return saved;
    }

    private void broadcastChannels(PrivateGroup group) {
        List<Long> members = group.getMembers().stream().map(User::getId).toList();
        eventPublisher.publishEvent(new RealtimeEvent(this, members,
                Map.of("type", "group.channels", "groupId", group.getId())));
    }

    static String normalizeName(String raw) {
        String name = slug(raw);
        if (name.length() < 2) {
            throw new BadRequestException("Usa letras o números en el nombre del canal");
        }
        return name;
    }

    private static String slug(String raw) {
        String clean = Normalizer.normalize(raw == null ? "" : raw, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
        return clean.length() > 40 ? clean.substring(0, 40) : clean;
    }

    private static void ensureUniqueName(List<GroupChannel> channels, String name, Long ignoreId) {
        boolean exists = channels.stream()
                .anyMatch(channel -> channel.getName().equals(name) && !channel.getId().equals(ignoreId));
        if (exists) {
            throw new DuplicateResourceException("Ya existe un canal #" + name);
        }
    }

    private static String cleanTopic(String topic) {
        return topic == null || topic.isBlank() ? null : topic.trim();
    }

    private void requireMember(PrivateGroup group, User user) {
        if (!groupRepository.existsByIdAndMembersId(group.getId(), user.getId())) {
            throw new ForbiddenException("Debes ser miembro del grupo");
        }
    }

    private static void requireManager(PrivateGroup group, User user) {
        if (!group.canManage(user) && !user.hasRole(RoleName.ROLE_ADMIN)) {
            throw new ForbiddenException("Solo el creador o un moderador del grupo puede administrar los canales");
        }
    }

    private PrivateGroup findGroup(Long groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Grupo no encontrado"));
    }

    private GroupChannel findChannel(Long groupId, Long channelId) {
        return channelRepository.findById(channelId)
                .filter(channel -> channel.getGroup().getId().equals(groupId))
                .orElseThrow(() -> new ResourceNotFoundException("Canal no encontrado"));
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
    }

    private static ChannelResponseDTO toResponse(GroupChannel channel) {
        return toResponse(channel, 0, 0);
    }

    private static ChannelResponseDTO toResponse(GroupChannel channel, long unread, long mentions) {
        return new ChannelResponseDTO(channel.getId(), channel.getName(), channel.getTopic(), channel.getPosition(),
                channel.isReadOnly(), channel.slowMode(), unread, mentions);
    }
}

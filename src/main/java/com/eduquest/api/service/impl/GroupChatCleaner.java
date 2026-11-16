package com.eduquest.api.service.impl;

import com.eduquest.api.repository.GroupChannelRepository;
import com.eduquest.api.repository.MessageReactionRepository;
import com.eduquest.api.repository.PollRepository;
import org.springframework.stereotype.Component;

@Component
public class GroupChatCleaner {

    private final MessageReactionRepository reactionRepository;
    private final PollRepository pollRepository;
    private final GroupChannelRepository channelRepository;

    public GroupChatCleaner(MessageReactionRepository reactionRepository,
                            PollRepository pollRepository,
                            GroupChannelRepository channelRepository) {
        this.reactionRepository = reactionRepository;
        this.pollRepository = pollRepository;
        this.channelRepository = channelRepository;
    }

    public void beforeMessages(Long groupId) {
        reactionRepository.deleteByGroupId(groupId);
    }

    public void afterMessages(Long groupId) {
        pollRepository.deleteVotesByGroupId(groupId);
        pollRepository.deleteOptionsByGroupId(groupId);
        pollRepository.deleteByGroupId(groupId);
        channelRepository.deleteByGroupId(groupId);
    }
}

package com.eduquest.api.service.impl;

import com.eduquest.api.dto.response.GlobalSearchDTO;
import com.eduquest.api.dto.response.SearchHitDTO;
import com.eduquest.api.dto.response.UserResponseDTO;
import com.eduquest.api.dto.response.UserSummaryDTO;
import com.eduquest.api.entity.User;
import com.eduquest.api.exception.ResourceNotFoundException;
import com.eduquest.api.repository.DocumentRepository;
import com.eduquest.api.repository.ForumQuestionRepository;
import com.eduquest.api.repository.PrivateGroupRepository;
import com.eduquest.api.repository.UserRepository;
import com.eduquest.api.service.SearchService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
public class SearchServiceImpl implements SearchService {

    static final int LIMIT = 5;

    private final DocumentRepository documentRepository;
    private final PrivateGroupRepository groupRepository;
    private final ForumQuestionRepository questionRepository;
    private final UserRepository userRepository;

    public SearchServiceImpl(DocumentRepository documentRepository, PrivateGroupRepository groupRepository,
                             ForumQuestionRepository questionRepository, UserRepository userRepository) {
        this.documentRepository = documentRepository;
        this.groupRepository = groupRepository;
        this.questionRepository = questionRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public GlobalSearchDTO search(String query, String username) {
        User me = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        String text = query == null ? "" : query.trim().replaceFirst("^@", "").toLowerCase(Locale.ROOT);
        if (text.length() < 2) {
            return new GlobalSearchDTO(List.of(), List.of(), List.of(), List.of());
        }
        String pattern = "%" + GroupCommunityServiceImpl.likeEscape(text) + "%";
        PageRequest page = PageRequest.of(0, LIMIT);
        List<SearchHitDTO> documents = documentRepository.searchPublicDocuments(pattern, null, null, null, page).stream()
                .map(document -> new SearchHitDTO(document.getId(), document.getTitle(), join(document.getCourse(), document.getUniversity())))
                .toList();
        List<SearchHitDTO> groups = groupRepository.searchForUser(pattern, me.getId(), page).stream()
                .map(group -> new SearchHitDTO(group.getId(), group.getName(), group.getMembers().size() + " integrantes"))
                .toList();
        List<SearchHitDTO> questions = questionRepository.search(pattern, "", "all", page).stream()
                .map(question -> new SearchHitDTO(question.getId(), question.getTitle(), question.getCourse()))
                .toList();
        List<UserSummaryDTO> users = userRepository.searchActive(pattern, me.getId(), page).stream()
                .map(user -> new UserSummaryDTO(user.getId(), user.getUsername(),
                        UserResponseDTO.avatarUrlFor(user.getId(), user.getAvatarVersion()), user.isAdvisor()))
                .toList();
        return new GlobalSearchDTO(documents, groups, questions, users);
    }

    private static String join(String first, String second) {
        if (first == null || first.isBlank()) {
            return second;
        }
        return second == null || second.isBlank() ? first : first + " · " + second;
    }
}

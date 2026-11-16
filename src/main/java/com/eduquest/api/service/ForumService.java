package com.eduquest.api.service;

import com.eduquest.api.dto.request.ForumQuestionRequestDTO;
import com.eduquest.api.dto.response.ForumAnswerResponseDTO;
import com.eduquest.api.dto.response.ForumQuestionResponseDTO;
import com.eduquest.api.dto.response.ForumThreadResponseDTO;
import com.eduquest.api.dto.response.PageResponseDTO;
import org.springframework.data.domain.Pageable;

public interface ForumService {
    PageResponseDTO<ForumQuestionResponseDTO> search(String username, String search, String course, String filter,
                                                     Pageable pageable);
    ForumQuestionResponseDTO ask(String username, ForumQuestionRequestDTO request);
    ForumThreadResponseDTO getThread(String username, Long questionId);
    void deleteQuestion(String username, Long questionId);
    ForumAnswerResponseDTO answer(String username, Long questionId, String body);
    void deleteAnswer(String username, Long answerId);
    ForumAnswerResponseDTO vote(String username, Long answerId, int value);
    ForumThreadResponseDTO accept(String username, Long questionId, Long answerId);
}

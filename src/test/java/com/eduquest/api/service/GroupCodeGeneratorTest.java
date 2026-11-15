package com.eduquest.api.service;

import com.eduquest.api.repository.PrivateGroupRepository;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GroupCodeGeneratorTest {

    @Test
    void nextCode_returnsSixCharsWithoutAmbiguousLetters() {
        PrivateGroupRepository repository = mock(PrivateGroupRepository.class);
        GroupCodeGenerator generator = new GroupCodeGenerator(repository);

        String code = generator.nextCode();

        assertThat(code).hasSize(6).matches("[A-HJKMNP-Z2-9]{6}");
    }

    @Test
    void nextCode_retriesWhenCodeAlreadyExists() {
        PrivateGroupRepository repository = mock(PrivateGroupRepository.class);
        when(repository.existsByCode(anyString())).thenReturn(true, false);
        GroupCodeGenerator generator = new GroupCodeGenerator(repository);

        generator.nextCode();

        verify(repository, times(2)).existsByCode(anyString());
    }

    @Test
    void nextInviteToken_returnsDifferentUrlSafeTokens() {
        GroupCodeGenerator generator = new GroupCodeGenerator(mock(PrivateGroupRepository.class));

        String first = generator.nextInviteToken();
        String second = generator.nextInviteToken();

        assertThat(first).hasSize(24).matches("[A-Za-z0-9_-]+");
        assertThat(second).isNotEqualTo(first);
    }
}

package com.eduquest.api.config;

import com.eduquest.api.entity.GroupVisibility;
import com.eduquest.api.entity.PrivateGroup;
import com.eduquest.api.repository.PrivateGroupRepository;
import com.eduquest.api.service.GroupCodeGenerator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GroupCodeInitializerTest {

    @Test
    void run_assignsCodesAndDefaultVisibility() {
        PrivateGroupRepository repository = mock(PrivateGroupRepository.class);
        GroupCodeGenerator generator = mock(GroupCodeGenerator.class);

        PrivateGroup legacy = new PrivateGroup();
        legacy.setVisibility(null);
        PrivateGroup closed = new PrivateGroup();
        closed.setVisibility(GroupVisibility.PRIVATE);

        when(repository.findByCodeIsNull()).thenReturn(List.of(legacy, closed));
        when(generator.nextCode()).thenReturn("AAAAAA", "BBBBBB");

        new GroupCodeInitializer(repository, generator).run();

        assertThat(legacy.getCode()).isEqualTo("AAAAAA");
        assertThat(legacy.getVisibility()).isEqualTo(GroupVisibility.PUBLIC);
        assertThat(closed.getCode()).isEqualTo("BBBBBB");
        assertThat(closed.getVisibility()).isEqualTo(GroupVisibility.PRIVATE);
        verify(repository).saveAll(List.of(legacy, closed));
    }
}

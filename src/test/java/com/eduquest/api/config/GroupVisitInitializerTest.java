package com.eduquest.api.config;

import com.eduquest.api.entity.GroupVisit;
import com.eduquest.api.entity.PrivateGroup;
import com.eduquest.api.entity.User;
import com.eduquest.api.repository.GroupVisitRepository;
import com.eduquest.api.repository.PrivateGroupRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GroupVisitInitializerTest {

    @Test
    @SuppressWarnings("unchecked")
    void run_createsVisitsOnlyForMembersWithoutOne() {
        PrivateGroupRepository groupRepository = mock(PrivateGroupRepository.class);
        GroupVisitRepository visitRepository = mock(GroupVisitRepository.class);

        User alice = new User();
        alice.setId(1L);
        User bob = new User();
        bob.setId(2L);

        PrivateGroup group = new PrivateGroup();
        group.setId(10L);
        group.getMembers().add(alice);
        group.getMembers().add(bob);

        when(groupRepository.findAll()).thenReturn(List.of(group));
        when(visitRepository.existsByUserIdAndGroupId(1L, 10L)).thenReturn(true);
        when(visitRepository.existsByUserIdAndGroupId(2L, 10L)).thenReturn(false);

        new GroupVisitInitializer(groupRepository, visitRepository).run();

        ArgumentCaptor<List<GroupVisit>> captor = ArgumentCaptor.forClass(List.class);
        verify(visitRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().get(0).getUser()).isSameAs(bob);
        assertThat(captor.getValue().get(0).getLastSeenAt()).isNotNull();
    }
}

package com.eduquest.api.service.impl;

import com.eduquest.api.dto.request.GroupRequestDTO;
import com.eduquest.api.dto.response.GroupResponseDTO;
import com.eduquest.api.dto.response.PageResponseDTO;
import com.eduquest.api.entity.PrivateGroup;
import com.eduquest.api.entity.User;
import com.eduquest.api.exception.ResourceNotFoundException;
import com.eduquest.api.repository.PrivateGroupRepository;
import com.eduquest.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GroupServiceImplTest {

    @Mock PrivateGroupRepository groupRepository;
    @Mock UserRepository userRepository;

    @InjectMocks GroupServiceImpl groupService;

    private User alice;

    @BeforeEach
    void setUp() {
        alice = new User();
        alice.setId(1L);
        alice.setUsername("alice");
        alice.setEmail("alice@utec.edu.pe");
    }

    @Test
    void createGroup_withExistingUser_createsAndAddsMember() {
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(groupRepository.save(any(PrivateGroup.class))).thenAnswer(invocation -> {
            PrivateGroup saved = invocation.getArgument(0);
            saved.setId(5L);
            return saved;
        });

        GroupRequestDTO request = new GroupRequestDTO();
        request.setName("Grupo de Calculo");
        request.setDescription("Calculo I");

        GroupResponseDTO response = groupService.createGroup(request, "alice");

        assertThat(response.getId()).isEqualTo(5L);
        assertThat(response.getName()).isEqualTo("Grupo de Calculo");
        assertThat(response.getCreatorUsername()).isEqualTo("alice");
    }

    @Test
    void createGroup_withUnknownUser_throwsNotFound() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        GroupRequestDTO request = new GroupRequestDTO();
        request.setName("G");
        request.setDescription("D");

        assertThatThrownBy(() -> groupService.createGroup(request, "ghost"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getAllGroups_returnsPageAndFallsBackCreatorLabel() {
        PrivateGroup withMember = new PrivateGroup();
        withMember.setId(1L);
        withMember.setName("Con miembro");
        withMember.getMembers().add(alice);

        PrivateGroup empty = new PrivateGroup();
        empty.setId(2L);
        empty.setName("Vacio");

        Pageable pageable = PageRequest.of(0, 10);
        when(groupRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(withMember, empty), pageable, 2));

        PageResponseDTO<GroupResponseDTO> page = groupService.getAllGroups(pageable);

        assertThat(page.content()).hasSize(2);
        assertThat(page.content().get(0).getCreatorUsername()).isEqualTo("alice");
        assertThat(page.content().get(1).getCreatorUsername()).isEqualTo("Sin miembros");
    }
}
package com.eduquest.api.service.impl;

import com.eduquest.api.dto.request.GroupRequestDTO;
import com.eduquest.api.dto.response.GroupResponseDTO;
import com.eduquest.api.dto.response.PageResponseDTO;
import com.eduquest.api.entity.PrivateGroup;
import com.eduquest.api.entity.User;
import com.eduquest.api.exception.ResourceNotFoundException;
import com.eduquest.api.repository.PrivateGroupRepository;
import com.eduquest.api.repository.UserRepository;
import com.eduquest.api.service.GroupService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class GroupServiceImpl implements GroupService {

    private final PrivateGroupRepository groupRepository;
    private final UserRepository userRepository;

    public GroupServiceImpl(PrivateGroupRepository groupRepository, UserRepository userRepository) {
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public GroupResponseDTO createGroup(GroupRequestDTO request, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        PrivateGroup group = new PrivateGroup();
        group.setName(request.getName());
        group.setDescription(request.getDescription());

        // Asociamos al usuario a la tabla intermedia de miembros del grupo
        group.getMembers().add(user);

        PrivateGroup savedGroup = groupRepository.save(group);

        GroupResponseDTO response = new GroupResponseDTO();
        response.setId(savedGroup.getId());
        response.setName(savedGroup.getName());
        response.setDescription(savedGroup.getDescription());
        response.setCreatorUsername(user.getUsername());

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<GroupResponseDTO> getAllGroups(Pageable pageable) {
        Page<PrivateGroup> groupPage = groupRepository.findAll(pageable);
        List<GroupResponseDTO> content = groupPage.getContent().stream().map(group -> {
            GroupResponseDTO dto = new GroupResponseDTO();
            dto.setId(group.getId());
            dto.setName(group.getName());
            dto.setDescription(group.getDescription());
            // Tomamos el primer miembro como referencia del creador principal
            String creator = group.getMembers().isEmpty() ? "Sin miembros" : group.getMembers().iterator().next().getUsername();
            dto.setCreatorUsername(creator);
            return dto;
        }).collect(Collectors.toList());
        return PageResponseDTO.of(content, groupPage.getNumber(), groupPage.getSize(), groupPage.getTotalElements());
    }
}
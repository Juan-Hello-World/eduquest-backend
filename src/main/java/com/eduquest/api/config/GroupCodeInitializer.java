package com.eduquest.api.config;

import com.eduquest.api.entity.GroupVisibility;
import com.eduquest.api.entity.PrivateGroup;
import com.eduquest.api.repository.PrivateGroupRepository;
import com.eduquest.api.service.GroupCodeGenerator;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class GroupCodeInitializer implements CommandLineRunner {

    private final PrivateGroupRepository groupRepository;
    private final GroupCodeGenerator codeGenerator;

    public GroupCodeInitializer(PrivateGroupRepository groupRepository, GroupCodeGenerator codeGenerator) {
        this.groupRepository = groupRepository;
        this.codeGenerator = codeGenerator;
    }

    @Override
    @Transactional
    public void run(String @NonNull ... args) {
        List<PrivateGroup> pending = groupRepository.findByCodeIsNull();
        for (PrivateGroup group : pending) {
            group.setCode(codeGenerator.nextCode());
            if (group.getVisibility() == null) {
                group.setVisibility(GroupVisibility.PUBLIC);
            }
        }
        groupRepository.saveAll(pending);
    }
}

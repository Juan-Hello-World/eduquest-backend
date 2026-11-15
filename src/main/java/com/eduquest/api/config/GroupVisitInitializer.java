package com.eduquest.api.config;

import com.eduquest.api.entity.GroupVisit;
import com.eduquest.api.entity.PrivateGroup;
import com.eduquest.api.entity.User;
import com.eduquest.api.repository.GroupVisitRepository;
import com.eduquest.api.repository.PrivateGroupRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class GroupVisitInitializer implements CommandLineRunner {

    private final PrivateGroupRepository groupRepository;
    private final GroupVisitRepository visitRepository;

    public GroupVisitInitializer(PrivateGroupRepository groupRepository, GroupVisitRepository visitRepository) {
        this.groupRepository = groupRepository;
        this.visitRepository = visitRepository;
    }

    @Override
    @Transactional
    public void run(String @NonNull ... args) {
        LocalDateTime now = LocalDateTime.now();
        List<GroupVisit> missing = new ArrayList<>();
        for (PrivateGroup group : groupRepository.findAll()) {
            for (User member : group.getMembers()) {
                if (!visitRepository.existsByUserIdAndGroupId(member.getId(), group.getId())) {
                    missing.add(new GroupVisit(member, group, now));
                }
            }
        }
        visitRepository.saveAll(missing);
    }
}

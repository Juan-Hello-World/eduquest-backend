package com.eduquest.api.repository;

import com.eduquest.api.entity.PrivateGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PrivateGroupRepository extends JpaRepository<PrivateGroup, Long> {
}
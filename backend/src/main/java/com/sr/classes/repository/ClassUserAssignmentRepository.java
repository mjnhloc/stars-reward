package com.sr.classes.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sr.classes.entity.ClassUserAssignmentEntity;

public interface ClassUserAssignmentRepository
    extends JpaRepository<ClassUserAssignmentEntity, ClassUserAssignmentEntity.Key> {

    boolean existsByUserIdAndClassId(UUID userId, UUID classId);

    boolean existsByUserIdAndClassIdAndCanAdjustTrue(UUID userId, UUID classId);
}

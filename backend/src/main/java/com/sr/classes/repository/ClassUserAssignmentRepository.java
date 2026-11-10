package com.sr.classes.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sr.classes.entity.ClassUserAssignmentEntity;

public interface ClassUserAssignmentRepository
    extends JpaRepository<ClassUserAssignmentEntity, ClassUserAssignmentEntity.Key> {

    boolean existsByUserIdAndClassId(UUID userId, UUID classId);

    boolean existsByUserIdAndClassIdAndCanAdjustTrue(UUID userId, UUID classId);

    List<ClassUserAssignmentEntity> findByUserIdIn(Collection<UUID> userIds);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "DELETE FROM class_user_assignments WHERE class_id = :classId", nativeQuery = true)
    int deleteAllOfClass(@Param("classId") UUID classId);

    @Modifying(flushAutomatically = true)
    @Query(value = "DELETE FROM class_user_assignments WHERE user_id = :userId", nativeQuery = true)
    int deleteAllOfUser(@Param("userId") UUID userId);
}

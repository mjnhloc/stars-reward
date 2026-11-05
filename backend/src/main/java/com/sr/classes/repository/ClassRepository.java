package com.sr.classes.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sr.classes.dto.response.ClassResponse;
import com.sr.classes.entity.ClassEntity;

import jakarta.persistence.LockModeType;

public interface ClassRepository extends JpaRepository<ClassEntity, UUID> {

    /*
     * Active classes with number of studying students and their total stars
     */
    @Query("""
        SELECT new com.sr.classes.dto.response.ClassResponse(
            c.id,
            c.code,
            c.name,
            CAST(COUNT(e.studentId) AS Integer),
            CAST(COALESCE(SUM(b.totalStars), 0) AS Integer)
        )
        FROM ClassEntity c
        LEFT JOIN ClassEnrollmentEntity e
            ON e.classId = c.id
            AND e.endedAt IS NULL
        LEFT JOIN StudentStarBalanceEntity b
            ON b.studentId = e.studentId
        WHERE c.archivedAt IS NULL
        GROUP BY c.id, c.code, c.name
        ORDER BY c.name
        """)
    List<ClassResponse> findActiveClassSummaries();

    Optional<ClassEntity> findByIdAndArchivedAtIsNull(UUID id);

    List<ClassEntity> findByArchivedAtIsNull();

    /*
     * SELECT ... FOR UPDATE
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT c
        FROM ClassEntity c
        WHERE c.id = :classId
            AND c.archivedAt IS NULL
        """)
    Optional<ClassEntity> findActiveForUpdate(@Param("classId") UUID classId);

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, UUID id);
}

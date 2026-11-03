package com.sr.student.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sr.student.entity.ClassEnrollmentEntity;

public interface ClassEnrollmentRepository extends JpaRepository<ClassEnrollmentEntity, UUID> {

    boolean existsByStudentIdAndClassIdAndEndedAtIsNull(UUID studentId, UUID classId);

    Optional<ClassEnrollmentEntity> findByStudentIdAndEndedAtIsNull(UUID studentId);

    /*
     * Number of active students currently studying in a class
     */
    @Query("""
        SELECT COUNT(e)
        FROM ClassEnrollmentEntity e
        JOIN StudentEntity s
            ON s.id = e.studentId
        WHERE e.classId = :classId
            AND e.endedAt IS NULL
            AND s.archivedAt IS NULL
        """)
    long countActiveStudents(
        @Param("classId")
        UUID classId
    );
}

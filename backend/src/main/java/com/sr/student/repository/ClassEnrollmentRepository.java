package com.sr.student.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sr.student.entity.ClassEnrollmentEntity;

public interface ClassEnrollmentRepository extends JpaRepository<ClassEnrollmentEntity, UUID> {

    boolean existsByStudentIdAndClassIdAndEndedAtIsNull(UUID studentId, UUID classId);

    Optional<ClassEnrollmentEntity> findByStudentIdAndEndedAtIsNull(UUID studentId);

    List<ClassEnrollmentEntity> findByStudentIdInAndEndedAtIsNull(Collection<UUID> studentIds);

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

    /*
     * Latest enrollment of a student, the class a deleted student is restored to
     */
    Optional<ClassEnrollmentEntity> findFirstByStudentIdOrderByStartedAtDesc(UUID studentId);

    /*
     * Enrollment before the current one, a new start date cannot be before it ended
     */
    Optional<ClassEnrollmentEntity> findFirstByStudentIdAndEndedAtIsNotNullOrderByEndedAtDesc(UUID studentId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "DELETE FROM class_enrollments WHERE student_id = :studentId", nativeQuery = true)
    int deleteAllOfStudent(@Param("studentId") UUID studentId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "DELETE FROM class_enrollments WHERE class_id = :classId", nativeQuery = true)
    int deleteAllOfClass(@Param("classId") UUID classId);
}

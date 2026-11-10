package com.sr.student.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sr.student.dto.response.StudentSummary;
import com.sr.student.dto.response.StudentTrashResponse;
import com.sr.student.entity.StudentEntity;

import jakarta.persistence.LockModeType;

public interface StudentRepository extends JpaRepository<StudentEntity, UUID> {

    /*
     * Search active students in every class by name or student code
     */
    @Query("""
        SELECT new com.sr.student.dto.response.StudentSummary(
            s.id,
            s.studentCode,
            s.fullName,
            c.id,
            c.name,
            COALESCE(b.totalStars, 0),
            e.startedAt
        )
        FROM StudentEntity s
        JOIN ClassEnrollmentEntity e
            ON e.studentId = s.id
            AND e.endedAt IS NULL
        JOIN ClassEntity c
            ON c.id = e.classId
        LEFT JOIN StudentStarBalanceEntity b
            ON b.studentId = s.id
        WHERE s.archivedAt IS NULL
            AND (
                LOWER(s.fullName) LIKE LOWER(:query)
                OR LOWER(s.studentCode) LIKE LOWER(:query)
            )
        ORDER BY s.fullName
        """)
    List<StudentSummary> searchActiveStudents(
        @Param("query")
        String query, Limit limit
    );

    /*
     * Same as searchActiveStudents but only in one class
     */
    @Query("""
        SELECT new com.sr.student.dto.response.StudentSummary(
            s.id,
            s.studentCode,
            s.fullName,
            c.id,
            c.name,
            COALESCE(b.totalStars, 0),
            e.startedAt
        )
        FROM StudentEntity s
        JOIN ClassEnrollmentEntity e
            ON e.studentId = s.id
            AND e.endedAt IS NULL
        JOIN ClassEntity c
            ON c.id = e.classId
        LEFT JOIN StudentStarBalanceEntity b
            ON b.studentId = s.id
        WHERE s.archivedAt IS NULL
            AND c.id = :classId
            AND (
                LOWER(s.fullName) LIKE LOWER(:query)
                OR LOWER(s.studentCode) LIKE LOWER(:query)
            )
        ORDER BY s.fullName
        """)
    List<StudentSummary> searchActiveStudentsInClass(
        @Param("classId")
        UUID classId,
        @Param("query")
        String query,
        Limit limit
    );

    /*
     * SELECT ... FOR UPDATE
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT s
        FROM StudentEntity s
        WHERE s.id = :studentId
            AND s.archivedAt IS NULL
        """)
    Optional<StudentEntity> findActiveForUpdate(
        @Param("studentId")
        UUID studentId
    );

    boolean existsByStudentCode(String studentCode);

    List<StudentEntity> findByStudentCodeIn(Collection<String> studentCodes);

    boolean existsByStudentCodeAndIdNot(String studentCode, UUID id);

    /*
     * Deleted students with the class of their latest enrollment, newest first
     */
    @Query("""
        SELECT new com.sr.student.dto.response.StudentTrashResponse(
            s.id,
            s.studentCode,
            s.fullName,
            c.id,
            c.name,
            CASE WHEN c.archivedAt IS NULL THEN false ELSE true END,
            s.archivedAt,
            COALESCE(b.totalStars, 0)
        )
        FROM StudentEntity s
        LEFT JOIN ClassEnrollmentEntity e
            ON e.studentId = s.id
        LEFT JOIN ClassEntity c
            ON c.id = e.classId
        LEFT JOIN StudentStarBalanceEntity b
            ON b.studentId = s.id
        WHERE s.archivedAt IS NOT NULL
            AND (
                e.id IS NULL
                OR e.startedAt = (
                    SELECT MAX(e2.startedAt)
                    FROM ClassEnrollmentEntity e2
                    WHERE e2.studentId = s.id
                )
            )
        ORDER BY s.archivedAt DESC
        """)
    List<StudentTrashResponse> findTrash();

    /*
     * SELECT ... FOR UPDATE of a student in the trash
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT s
        FROM StudentEntity s
        WHERE s.id = :studentId
            AND s.archivedAt IS NOT NULL
        """)
    Optional<StudentEntity> findArchivedForUpdate(@Param("studentId") UUID studentId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "DELETE FROM students WHERE id = :studentId", nativeQuery = true)
    int deletePermanently(@Param("studentId") UUID studentId);
}

package com.sr.student.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sr.dashboard.dto.response.DashboardResponse;
import com.sr.student.entity.StudentStarBalanceEntity;

import jakarta.persistence.LockModeType;

public interface StudentStarBalanceRepository extends JpaRepository<StudentStarBalanceEntity, UUID> {

    /*
     * Lock balance row until transaction finish (SELECT ... FOR UPDATE)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT b
        FROM StudentStarBalanceEntity b
        WHERE b.studentId = :studentId
        """)
    Optional<StudentStarBalanceEntity> findForUpdate(@Param("studentId") UUID studentId);

    /*
     * Total students and stars, archived students are excluded
     */
    @Query("""
        SELECT new com.sr.dashboard.dto.response.DashboardResponse(
            COUNT(b),
            COALESCE(SUM(b.totalStars), 0L)
        )
        FROM StudentStarBalanceEntity b
        JOIN StudentEntity s
            ON s.id = b.studentId
        WHERE s.archivedAt IS NULL
        """)
    DashboardResponse getDashboardSummary();

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "DELETE FROM student_star_balances WHERE student_id = :studentId", nativeQuery = true)
    int deleteOfStudent(@Param("studentId") UUID studentId);
}

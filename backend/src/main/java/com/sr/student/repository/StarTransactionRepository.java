package com.sr.student.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sr.student.dto.response.StudentTransactionResponse;
import com.sr.student.entity.StarTransactionEntity;

public interface StarTransactionRepository extends JpaRepository<StarTransactionEntity, UUID> {

    Optional<StarTransactionEntity> findByIdempotencyKey(String idempotencyKey);

    List<StarTransactionEntity> findByScoreIdInAndTransactionType(Collection<UUID> scoreIds, String transactionType);

    /*
     * Transaction history of a student, newest first
     */
    @Query("""
        SELECT new com.sr.student.dto.response.StudentTransactionResponse(
            t.id,
            t.transactionType,
            t.sourceType,
            t.starDelta,
            t.balanceBefore,
            t.balanceAfter,
            t.reasonDetail,
            t.createdAt,
            u.displayName,
            c.name
        )
        FROM StarTransactionEntity t
        LEFT JOIN UserEntity u
            ON u.id = t.performedBy
        LEFT JOIN ClassEntity c
            ON c.id = t.classId
        WHERE t.studentId = :studentId
        ORDER BY t.createdAt DESC
        """)
    List<StudentTransactionResponse> findHistory(@Param("studentId") UUID studentId, Limit limit);

    /*
     * Permanently delete the whole ledger of a student. Reversals point to rows of
     * the same student, foreign keys are checked at the end of the statement
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "DELETE FROM star_transactions WHERE student_id = :studentId", nativeQuery = true)
    int deleteAllOfStudent(@Param("studentId") UUID studentId);

    /*
     * Keep the history of students who moved away when their old class is deleted
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "UPDATE star_transactions SET class_id = NULL WHERE class_id = :classId", nativeQuery = true)
    int detachClass(@Param("classId") UUID classId);
}

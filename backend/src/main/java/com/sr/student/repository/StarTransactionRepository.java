package com.sr.student.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sr.student.dto.response.StudentTransactionResponse;
import com.sr.student.entity.StarTransactionEntity;

public interface StarTransactionRepository extends JpaRepository<StarTransactionEntity, UUID> {

    Optional<StarTransactionEntity> findByIdempotencyKey(String idempotencyKey);

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
}

package com.sr.student.service.impl;

import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.sr.auth.service.AuthorizationService;
import com.sr.reward.service.RewardCalculationService;
import com.sr.student.dto.request.StudentStarsAdjustmentRequest;
import com.sr.student.dto.response.StudentResponse;
import com.sr.student.dto.response.StudentScoreResponse;
import com.sr.student.dto.response.StudentStarsAdjustmentResponse;
import com.sr.student.dto.response.StudentSummary;
import com.sr.student.dto.response.StudentTransactionResponse;
import com.sr.student.service.StudentService;

@Service
public class StudentServiceImpl implements StudentService {

    private final JdbcClient jdbcClient;
    private final AuthorizationService auth;
    private final RewardCalculationService rewardCalculationService;

    public StudentServiceImpl(
        JdbcClient jdbcClient,
        AuthorizationService auth,
        RewardCalculationService rcs
    ) {
        this.jdbcClient = jdbcClient;
        this.auth = auth;
        this.rewardCalculationService = rcs;
    }

    /*
     * Function search after name or student code for every character typed
     */
    @Override
    public List<StudentResponse> getStudents(UUID classId, String query) {
        String searchQuery = query == null ? "" : query.trim();

        StringBuilder sql = new StringBuilder("""
                    SELECT
                        s.id,
                        s.student_code,
                        s.full_name,
                        c.id AS class_id,
                        c.name AS class_name,
                        COALESCE(b.total_stars, 0) AS total_stars
                    FROM students s
                    JOIN class_enrollments e
                        ON e.student_id = s.id
                        AND e.ended_at IS NULL
                    JOIN classes c
                        ON c.id = e.class_id
                    LEFT JOIN student_star_balances b
                        ON b.student_id = s.id
                    WHERE s.archived_at IS NULL
            """);

        if (classId != null) {
            sql.append("""

                AND c.id = :classId
                """);
        }

        sql.append("""

            AND (
                LOWER(s.full_name) LIKE LOWER(:query)
                OR LOWER(s.student_code) LIKE LOWER(:query)
            )
            ORDER BY s.full_name
            LIMIT 500
            """);

        JdbcClient.StatementSpec statement = jdbcClient.sql(sql.toString())
            .param("query", "%" + searchQuery + "%");

        if (classId != null) {
            statement = statement.param("classId", classId);
        }

        List<StudentSummary> sm = statement.query(StudentSummary.class).list();

        return sm.stream().map(student -> {
            int totalStars = student.totalStars();

            return new StudentResponse(
                student.id(),
                student.studentCode(),
                student.fullName(),
                student.classId(),
                student.className(),
                totalStars,
                rewardCalculationService.calculateRedeemableStars(totalStars),
                rewardCalculationService.calculateRedeemableValue(totalStars),
                rewardCalculationService.calculateRemainingStars(totalStars)
            );
        }).toList();
    }

    /*
     * Function get student transaction history
     */
    @Override
    @Transactional(readOnly = true)
    public List<StudentTransactionResponse> getStudentTransactions(UUID studentId) {
        return jdbcClient.sql("""
                SELECT
                        t.id,
                        t.transaction_type,
                        t.source_type,
                        t.star_delta,
                        t.balance_before,
                        t.balance_after,
                        t.reason_detail,
                        t.created_at,
                        u.display_name
                    FROM star_transactions t
                    LEFT JOIN users u
                        ON u.id = t.performed_by
                    WHERE t.student_id = :studentId
                    ORDER BY t.created_at DESC
                    LIMIT 200
            """)
            .param("studentId", studentId)
            .query(StudentTransactionResponse.class)
            .list();
    }

    /*
     * Function to adjust student stars (add or substract)
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public StudentStarsAdjustmentResponse adjustStudentStars(
        UUID actorId,
        UUID studentId,
        StudentStarsAdjustmentRequest request,
        String idempotencyKey
    ) {
        if (request.stars() == 0) {
            throw new IllegalArgumentException("Số sao phải khác 0");
        }

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Thiếu Idempotency-Key");
        }

        /*
         * If key's already handled before, return the old one. This will prevent user
         * from double-clicking or frontend retry
         */
        StudentStarsAdjustmentResponse existingResult = findExistingAdjustment(idempotencyKey, studentId);
        if (existingResult != null) {
            return existingResult;
        }

        auth.requireAdjust(actorId, request.classId());

        boolean enrolled = jdbcClient.sql("""
                SELECT EXISTS(
                    SELECT 1
                    FROM class_enrollments
                    WHERE student_id = :studentId
                    AND class_id = :classId
                    AND ended_at IS NULL
                )
            """)
            .param("studentId", studentId)
            .param("classId", request.classId())
            .query(Boolean.class)
            .single();

        if (!enrolled) {
            throw new IllegalArgumentException("Học sinh không thuộc lớp đã chọn");
        }

        /*
         * Lock row balance until transaction finish
         */
        int balanceBefore = jdbcClient.sql("""
                SELECT total_stars
                FROM student_star_balances
                WHERE student_id = :studentId
                FOR UPDATE
            """)
            .param("studentId", studentId)
            .query(Integer.class)
            .optional()
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy số dư học sinh"));

        int balanceAfter = Math.addExact(balanceBefore, request.stars());
        if (balanceAfter < 0) {
            throw new IllegalArgumentException("Không thể trừ vì tổng ngôi sao sẽ nhỏ hơn 0");
        }

        UUID transactionId = UUID.randomUUID();
        String transactionType = request.stars() > 0 ? "EARN" : "DEDUCT";

        jdbcClient.sql("""
                INSERT INTO star_transactions (
                    id,
                    student_id,
                    class_id,
                    transaction_type,
                    source_type,
                    star_delta,
                    balance_before,
                    balance_after,
                    reason_detail,
                    performed_by,
                    idempotency_key,
                )
                VALUES (
                    :transactionId,
                    :studentId,
                    :classId,
                    :transactionType,
                    'MANUAL',
                    :starDelta,
                    :balanceBefore,
                    :balanceAfter,
                    :reasonDetail,
                    :performedBy,
                    :idempotencyKey,
                )
            """)
            .param("transactionId", transactionId)
            .param("studentId", studentId)
            .param("classId", request.classId())
            .param("transactionType", transactionType)
            .param("starDelta", request.stars())
            .param("balanceBefore", balanceBefore)
            .param("balanceAfter", balanceAfter)
            .param("reasonDetail", request.reasonDetail())
            .param("performedBy", actorId)
            .param("idempotencyKey", idempotencyKey)
            .update();

        int updatedRows = jdbcClient.sql("""
                UPDATE student_star_balances
                SET total_stars = :balanceAfter,
                    version = version + 1,
                    updated_at = CURRENT_TIMESTAMP
                WHERE student_id = :studentId
            """)
            .param("balanceAfter", balanceAfter)
            .param("studentId", studentId)
            .update();

        if (updatedRows != 1) {
            throw new IllegalStateException("Không thể cập nhật số dư học sinh");
        }

        /*
         * insert adjustment into audit logs
         */
        jdbcClient.sql("""
            INSERT INTO audit_logs (
                id,
                actor_id,
                action,
                entity_type,
                entity_id,
                details
            )
            VALUES (
                :id,
                :actorId,
                :action,
                'STAR_TRANSACTION',
                :transactionId,
                jsonb_build_object(
                    'studentId', CAST(:studentId AS TEXT),
                    'classId', CAST(:classId AS TEXT),
                    'transactionType', :transactionType,
                    'starDelta', :starDelta,
                    'balanceBefore', :balanceBefore,
                    'balanceAfter', :balanceAfter,
                    'reasonDetail', :reasonDetail
                )
            )
            """)
            .param("id", UUID.randomUUID())
            .param("actorId", actorId)
            .param("action", request.stars() > 0 ? "STARS_ADDED" : "STARS_DEDUCTED")
            .param("transactionId", transactionId)
            .param("studentId", studentId)
            .param("classId", request.classId())
            .param("transactionType", transactionType)
            .param("starDelta", request.stars())
            .param("balanceBefore", balanceBefore)
            .param("balanceAfter", balanceAfter)
            .param("reasonDetail", request.reasonDetail())
            .update();

        return new StudentStarsAdjustmentResponse(transactionId, studentId, request.stars(), balanceAfter);
    }

    /*
     * Support function to find if there is any existing adjustment with the same
     * idempotency-key
     */
    private StudentStarsAdjustmentResponse findExistingAdjustment(
        String idempotencyKey,
        UUID studentId
    ) {
        return jdbcClient.sql("""
            SELECT
                id,
                student_id,
                star_delta,
                balance_after
            FROM star_transactions
            WHERE idempotency_key = :idempotencyKey
            """)
            .param("idempotencyKey", idempotencyKey)
            .query((resultSet, rowNumber) -> {
                UUID existingStudentId = resultSet.getObject(
                    "student_id",
                    UUID.class
                );

                if (!studentId.equals(existingStudentId)) {
                    throw new IllegalArgumentException(
                        "Idempotency-Key đã được sử dụng "
                            + "cho một yêu cầu khác"
                    );
                }

                return new StudentStarsAdjustmentResponse(
                    resultSet.getObject(
                        "id",
                        UUID.class
                    ),
                    existingStudentId,
                    resultSet.getInt("star_delta"),
                    resultSet.getInt("balance_after")
                );
            })
            .optional()
            .orElse(null);
    }

    /*
     * Function to get all the scores detail for a student
     */
    @Override
    @Transactional(readOnly = true)
    public List<StudentScoreResponse> getStudentScores(
        UUID studentId
    ) {
        return jdbcClient.sql("""
            SELECT
                id,
                homework_score,
                test_score,
                final_score,
                awarded_stars,
                assessment_date,
                created_at
            FROM student_scores
            WHERE student_id = :studentId
            ORDER BY
                assessment_date DESC,
                created_at DESC
            """)
            .param("studentId", studentId)
            .query(StudentScoreResponse.class)
            .list();
    }
}

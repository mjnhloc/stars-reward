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
                    sc.homework_score,
                    sc.test_score,
                    sc.final_score,
                    COALESCE(b.total_stars, 0) AS total_stars
                FROM students s
                JOIN class_enrollments e
                    ON e.student_id = s.id
                    AND e.ended_at IS NULL
                JOIN classes c
                    ON c.id = e.class_id
                LEFT JOIN LATERAL (
                    SELECT
                        student_scores.homework_score,
                        student_scores.test_score,
                        student_scores.final_score
                    FROM student_scores
                    WHERE student_scores.student_id = s.id
                    ORDER BY
                        student_scores.assessment_date DESC,
                        student_scores.created_at DESC
                    LIMIT 1
                ) sc ON TRUE
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
                student.homeworkScore(),
                student.testScore(),
                student.finalScore(),
                totalStars,
                rewardCalculationService.calculateRedeemableStars(totalStars),
                rewardCalculationService.calculateRedeemableValue(totalStars),
                rewardCalculationService.calculateRemainingStars(totalStars)
            );
        })
        .toList();

    }

    @Override
    public List<StudentTransactionResponse> getStudentTransactions(UUID studentId) {
        StringBuilder sql = new StringBuilder("""
            SELECT
                    t.id,
                    t.star_delta,
                    t.balance_before,
                    t.balance_after,
                    t.reason_detail,
                    t.created_at,
                    u.display_name AS user_name
                FROM star_transactions t
                JOIN users u
                    ON u.id = t.performed_by
                WHERE t.student_id = :studentId
                ORDER BY t.created_at DESC
                LIMIT 200        
        """);

        JdbcClient.StatementSpec statement = jdbcClient.sql(sql.toString()).param("studentId", studentId);
        return statement.query(StudentTransactionResponse.class).list();
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public StudentStarsAdjustmentResponse adjustStudentStars(UUID actor, UUID student, StudentStarsAdjustmentRequest r, String key) {
        if (r.stars() == 0)
            throw new IllegalArgumentException("Số sao phải khác 0");

        if (key == null || key.isBlank())
            throw new IllegalArgumentException("Thiếu Idempotency-Key");

        auth.requireAdjust(actor, r.classId());
        boolean enrolled = jdbcClient.sql(
                "SELECT EXISTS(SELECT 1 FROM class_enrollments WHERE student_id=:s AND class_id=:c AND ended_at IS NULL)")
                .param("s", student).param("c", r.classId()).query(Boolean.class).single();

        if (!enrolled)
            throw new IllegalArgumentException("Học sinh không thuộc lớp đã chọn");

        int before = jdbcClient.sql("SELECT total_stars FROM student_star_balances WHERE student_id=:s FOR UPDATE")
                .param("s", student).query(Integer.class).optional()
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy số dư học sinh"));

        int after = Math.addExact(before, r.stars());
        if (after < 0)
            throw new IllegalArgumentException("Không thể trừ vì tổng ngôi sao sẽ nhỏ hơn 0");

        UUID id = UUID.randomUUID();

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
                        reason_code,
                        reason_detail,
                        performed_by,
                        idempotency_key,
                    )
                    VALUES (
                        :id,
                        :s,
                        :c,
                        :type,
                        'MANUAL',
                        :d,
                        :b,
                        :a,
                        :rc,
                        :rd,
                        :u,
                        :k,
                    )
                """)
                .param("id", id)
                .param("s", student)
                .param("c", r.classId())
                .param("type", r.stars() > 0 ? "EARN" : "DEDUCT")
                .param("d", r.stars())
                .param("b", before)
                .param("a", after)
                .param("rd", r.reasonDetail())
                .param("u", actor)
                .param("k", key)
                .update();

        jdbcClient.sql("""
                    UPDATE student_star_balances 
                    SET total_stars=:a,
                        version=version+1
                    WHERE student_id=:s
                """)
                .param("a", after)
                .param("s", student)
                .update();

        return new StudentStarsAdjustmentResponse(id, student, r.stars(), after);
    }
}

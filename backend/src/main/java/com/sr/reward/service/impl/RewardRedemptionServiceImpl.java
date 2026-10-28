package com.sr.reward.service.impl;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.sr.auth.service.AuthorizationService;
import com.sr.common.audit.dto.AuditLogCommand;
import com.sr.common.audit.service.AuditLogService;
import com.sr.reward.dto.request.RewardRedemptionRequest;
import com.sr.reward.dto.response.RewardRedemptionResponse;
import com.sr.reward.service.RewardCalculationService;
import com.sr.reward.service.RewardRedemptionService;
import com.sr.user.service.UserService;

@Service
public class RewardRedemptionServiceImpl implements RewardRedemptionService {

    private final JdbcClient jdbcClient;
    private final UserService userService;
    private final RewardCalculationService rewardCalculationService;
    private final AuthorizationService authorizationService;
    private final AuditLogService auditLogService;

    public RewardRedemptionServiceImpl(
        JdbcClient jdbcClient,
        UserService userService,
        RewardCalculationService rewardCalculationService,
        AuthorizationService authorizationService,
        AuditLogService auditLogService
    ) {
        this.jdbcClient = jdbcClient;
        this.userService = userService;
        this.rewardCalculationService = rewardCalculationService;
        this.authorizationService = authorizationService;
        this.auditLogService = auditLogService;
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public RewardRedemptionResponse redeem(UUID studentId, String idempotencyKey, RewardRedemptionRequest request) {
        validateRequest(studentId, idempotencyKey, request);

        /*
         * If user doesn't have right to modify the class, stop before locking balance
         */
        UUID actorId = userService.getUserId();
        authorizationService.requireAdjust(actorId, request.classId());

        /*
         * Check to see if there is a same existing redemption
         */
        RewardRedemptionResponse existing = jdbcClient.sql("""
            SELECT
                r.id AS redemption_id,
                r.student_id,
                r.stars_redeemed,
                r.amount_vnd,
                t.balance_after AS remaining_stars,
                r.status,
                r.created_at
            FROM star_transactions t
            JOIN reward_redemptions r
                ON r.id = t.redemption_id
            WHERE t.idempotency_key = :idempotencyKey
            """)
            .param(
                "idempotencyKey",
                idempotencyKey
            )
            .query((resultSet, rowNumber) -> {
                UUID existingStudentId = resultSet.getObject(
                    "student_id",
                    UUID.class
                );

                if (!studentId.equals(
                    existingStudentId
                )) {
                    throw new IllegalArgumentException(
                        "Idempotency-Key đã được sử dụng "
                            + "cho yêu cầu khác"
                    );
                }

                return new RewardRedemptionResponse(
                    resultSet.getObject(
                        "redemption_id",
                        UUID.class
                    ),
                    existingStudentId,
                    resultSet.getInt(
                        "stars_redeemed"
                    ),
                    resultSet.getLong(
                        "amount_vnd"
                    ),
                    resultSet.getInt(
                        "remaining_stars"
                    ),
                    resultSet.getString(
                        "status"
                    ),
                    resultSet.getObject(
                        "created_at",
                        OffsetDateTime.class
                    )
                );
            })
            .optional()
            .orElse(null);

        if (existing != null) {
            return existing;
        }

        validateActiveEnrollment(studentId, request.classId());

        /*
         * Lock balance so that 2 requests cannot redeem the same stars
         */
        int totalStars = jdbcClient.sql("""
                SELECT total_stars
                FROM student_star_balances
                WHERE student_id = :studentId
                FOR UPDATE
            """)
            .param("studentId", studentId)
            .query(Integer.class)
            .optional()
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tổng số sao của học sinh " + studentId));

        int redeemableStars = rewardCalculationService.calculateRedeemableStars(totalStars);
        long redeemableValue = rewardCalculationService.calculateRedeemableValue(totalStars);
        int remainingStars = rewardCalculationService.calculateRemainingStars(totalStars);

        UUID redemptionId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();

        OffsetDateTime now = OffsetDateTime.now();

        /*
         * Update insert reward redemption
         */

        int insertedRedemptionRows = jdbcClient.sql("""
                INSERT INTO reward_redemptions (
                    id,
                    student_id,
                    stars_redeemed,
                    stars_per_redemption,
                    value_per_redemption,
                    amount_vnd,
                    status,
                    created_by,
                    paid_at
                )
                VALUES (
                    :id,
                    :studentId,
                    :starsRedeemed,
                    :starsPerRedemption,
                    :valuePerRedemption,
                    :amountVnd,
                    'PAID',
                    :createdBy,
                    :paidAt
                )
            """)
            .param("id", redemptionId)
            .param("studentId", studentId)
            .param("starsRedeemed", redeemableStars)
            .param("starsPerRedemption", rewardCalculationService.getStarsPerRedemption())
            .param("valuePerRedemption", rewardCalculationService.getVndPerRedemption())
            .param("amountVnd", redeemableValue)
            .param("createdBy", actorId)
            .param("paidAt", now)
            .update();

        if (insertedRedemptionRows != 1) {
            throw new IllegalStateException(
                "Không thể tạo giao dịch đổi thưởng"
            );
        }

        /*
         * update insert new star transaction
         */
        int insertedTransaction = jdbcClient.sql("""
                INSERT INTO star_transactions (
                    id,
                    student_id,
                    class_id,
                    redemption_id,
                    transaction_type,
                    source_type,
                    star_delta,
                    balance_before,
                    balance_after,
                    reason_detail,
                    performed_by,
                    idempotency_key
                )
                VALUES (
                    :id,
                    :studentId,
                    :classId,
                    :redemptionId,
                    'REDEEM',
                    'REDEMPTION',
                    :starDelta,
                    :balanceBefore,
                    :balanceAfter,
                    :reasonDetail,
                    :performedBy,
                    :idempotencyKey
                )
            """)
            .param("id", transactionId)
            .param("studentId", studentId)
            .param("classId", request.classId())
            .param("redemptionId", redemptionId)
            .param("starDelta", -redeemableStars)
            .param("balanceBefore", totalStars)
            .param("balanceAfter", remainingStars)
            .param("reasonDetail", "Đổi " + redeemableStars + " sao thành " + redeemableValue + " VND")
            .param("performedBy", actorId)
            .param("idempotencyKey", idempotencyKey)
            .update();

        if (insertedTransaction != 1) {
            throw new IllegalStateException("Không thể tạo giao dịch trừ sao");
        }

        /*
         * update student star balance
         */
        int updatedBalanceRows = jdbcClient.sql("""
                UPDATE student_star_balances
                SET total_stars = :remainingStars,
                    version = version + 1,
                    updated_at = :updatedAt
                WHERE student_id = :studentId
            """)
            .param("remainingStars", remainingStars)
            .param("studentId", studentId)
            .param("updatedAt", now)
            .update();

        if (updatedBalanceRows != 1) {
            throw new IllegalStateException("Không thể cập nhật số sao của học sinh " + studentId);
        }

        auditLogService.record(
            new AuditLogCommand(
                actorId,
                "REWARD_REDEEMED",
                "REWARD_REDEMPTION",
                redemptionId,
                Map.of(
                    "studentId",
                    studentId.toString(),
                    "classId",
                    request.classId().toString(),
                    "transactionId",
                    transactionId.toString(),
                    "balanceBefore",
                    totalStars,
                    "starsRedeemed",
                    redeemableStars,
                    "balanceAfter",
                    remainingStars,
                    "amountVnd",
                    redeemableValue
                )
            )
        );

        return new RewardRedemptionResponse(
            redemptionId,
            studentId,
            redeemableStars,
            redeemableValue,
            remainingStars,
            "PAID",
            now
        );
    }

    /*
     * Helper function to validate if the student go to a specific class
     */
    private void validateActiveEnrollment(
        UUID studentId,
        UUID classId
    ) {
        boolean enrolled = jdbcClient.sql("""
            SELECT EXISTS (
                SELECT 1
                FROM class_enrollments
                WHERE student_id = :studentId
                  AND class_id = :classId
                  AND ended_at IS NULL
            )
            """)
            .param("studentId", studentId)
            .param("classId", classId)
            .query(Boolean.class)
            .single();

        if (!enrolled) {
            throw new IllegalArgumentException(
                "Học sinh không thuộc lớp đã chọn"
            );
        }
    }

    /*
     * Helper function to validate the request argument from frontend
     */
    private void validateRequest(
        UUID studentId,
        String idempotencyKey,
        RewardRedemptionRequest request
    ) {
        if (studentId == null) {
            throw new IllegalArgumentException(
                "Học sinh không được để trống"
            );
        }

        if (request == null
            || request.classId() == null) {
            throw new IllegalArgumentException(
                "Lớp học không được để trống"
            );
        }

        if (idempotencyKey == null
            || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException(
                "Thiếu Idempotency-Key"
            );
        }

        if (idempotencyKey.length() > 150) {
            throw new IllegalArgumentException(
                "Idempotency-Key không được vượt quá 150 ký tự"
            );
        }
    }
}

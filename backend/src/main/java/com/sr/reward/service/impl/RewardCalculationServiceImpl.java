package com.sr.reward.service.impl;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sr.reward.dto.request.RewardRedemptionRequest;
import com.sr.reward.dto.response.RewardRedemptionResponse;
import com.sr.reward.service.RewardCalculationService;
import com.sr.user.service.UserService;

@Service
public class RewardCalculationServiceImpl implements RewardCalculationService {
    
    private final int starsPerRedemption;
    private final int vndPerRedemption;
    private final JdbcClient jdbcClient;
    private final UserService userService;

    public RewardCalculationServiceImpl(
        @Value("${app.rewards.stars-per-redemption}") 
        int starsPerRedemption,
        @Value("${app.rewards.vnd-per-redemption}") 
        int vndPerRedemption,
        JdbcClient jdbcClient,
        UserService userService
    ) {
        this.starsPerRedemption = starsPerRedemption;
        this.vndPerRedemption = vndPerRedemption;
        this.jdbcClient = jdbcClient;
        this.userService = userService;
    }

    private void validate(int totalStars) {
        if (totalStars < 10) {
            throw new IllegalArgumentException("Tổng số sao chưa đủ 10 để đổi thưởng");
        }
    }

    @Override 
    public long calculateRedeemableValue(int totalStars) {
        validate(totalStars);
        long redemptionCount = totalStars / starsPerRedemption;
        
        return Math.multiplyExact(redemptionCount, vndPerRedemption);
    }

    @Override 
    public int calculateRedeemableStars(int totalStars) {
        validate(totalStars);

        return (totalStars / starsPerRedemption) * starsPerRedemption;
    }

    @Override 
    public int calculateRemainingStars(int totalStars) {
        return totalStars % starsPerRedemption;
    }

    @Transactional 
    @Override 
    public RewardRedemptionResponse redeem(UUID studentId, String idempotencyKey, RewardRedemptionRequest request) {
        int totalStars = jdbcClient.sql("""
            SELECT total_stars
            FROM student_star_balances s
            WHERE s.id = :studentId
            FOR UPDATE
        """)
        .param("studentId", studentId)
        .query(Integer.class)
        .optional()
        .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tổng số sao của học sinh " + studentId));

        int redeemableStars = calculateRedeemableStars(totalStars);
        long redeemableValue = calculateRedeemableValue(totalStars);
        int remainingStars = calculateRemainingStars(totalStars);

        UUID userId = userService.getUserId();

        UUID redemptionId = UUID.randomUUID();
        UUID transactionId = UUID.randomUUID();
        UUID auditLogId = UUID.randomUUID();

        OffsetDateTime now = OffsetDateTime.now();

        UUID classId = jdbcClient.sql("""
            SELECT class_id 
            FROM class_enrollments
            WHERE student_id = :studentId
                AND ended_at IS NULL
            FOR UPDATE        
        """)
        .param("studentId", studentId)
        .query(UUID.class)
        .optional()
        .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy lớp đang học của học sinh " + studentId));

        // Update reward redemption
        jdbcClient.sql("""
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
                :now
            )
        """)
        .param("id", redemptionId)
        .param("studentId", studentId)
        .param("starsRedeemed", redeemableStars)
        .param("starsPerRedemption", starsPerRedemption)
        .param("valuePerRedemption", vndPerRedemption)
        .param("amountVnd", redeemableValue)
        .param("createdBy", userId)
        .param("now", now)
        .update();

        // update star transaction
        jdbcClient.sql("""
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
        .param("id", redemptionId)
        .param("studentId", studentId)
        .param("classId", classId)
        .param("redemptionId", redemptionId)
        .param("starDelta", -redeemableStars)
        .param("balanceBefore", totalStars)
        .param("balanceAfter", remainingStars)
        .param("reasonDetail", "Đổi " + redeemableStars + " sao thành " + redeemableValue + " VND")
        .param("performedBy", userId)
        .param("idempotencyKey", idempotencyKey)
        .update();

        // update student star balance
        int updatedRows = jdbcClient.sql("""
            UPDATE student_star_balances
            SET total_stars = :remainingStars,
                version = version + 1
            WHERE student_id = :studentId        
        """)
        .param("remainingStars", remainingStars)
        .param("studentId", studentId)
        .update();

        if (updatedRows != 1) {
            throw new IllegalStateException("Không thể cập nhật số sao của học sinh " + studentId);
        }

        return new RewardRedemptionResponse(redemptionId, studentId, redeemableStars, redeemableValue, remainingStars, "PAID");
    }
}

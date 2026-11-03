package com.sr.reward.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sr.reward.dto.response.RewardRedemptionResponse;
import com.sr.reward.entity.RewardRedemptionEntity;

public interface RewardRedemptionRepository extends JpaRepository<RewardRedemptionEntity, UUID> {

    /*
     * Find a redemption that was already handled with the same idempotency key
     */
    @Query("""
        SELECT new com.sr.reward.dto.response.RewardRedemptionResponse(
            r.id,
            r.studentId,
            r.starsRedeemed,
            r.amountVnd,
            t.balanceAfter,
            r.status,
            r.createdAt
        )
        FROM StarTransactionEntity t
        JOIN RewardRedemptionEntity r
            ON r.id = t.redemptionId
        WHERE t.idempotencyKey = :idempotencyKey
        """)
    Optional<RewardRedemptionResponse> findByIdempotencyKey(@Param("idempotencyKey") String idempotencyKey);
}

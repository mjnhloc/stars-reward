package com.sr.reward.controller;

import java.util.UUID;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sr.reward.dto.request.RewardRedemptionRequest;
import com.sr.reward.dto.response.RewardRedemptionResponse;
import com.sr.reward.service.RewardCalculationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/reward")
public class RewardController {

    private final RewardCalculationService rewardCalculationService;

    public RewardController(
        RewardCalculationService rewardCalculationService
    ) {
        this.rewardCalculationService = rewardCalculationService;
    }

    @PostMapping("/{studentId}/redemptions")
    public RewardRedemptionResponse redeemReward(
        @PathVariable
        UUID studentId,
        @RequestHeader("Idempotency-Key")
        String idempotencyKey,
        @Valid
        @RequestBody
        RewardRedemptionRequest redemptionRequest
    ) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency-Key không được trống");
        }

        return rewardCalculationService.redeem(studentId, idempotencyKey, redemptionRequest);
    }
}

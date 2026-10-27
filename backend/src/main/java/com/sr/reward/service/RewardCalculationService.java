package com.sr.reward.service;

import java.util.UUID;

import com.sr.reward.dto.request.RewardRedemptionRequest;
import com.sr.reward.dto.response.RewardRedemptionResponse;

public interface RewardCalculationService {

    long calculateRedeemableValue(int totalStars);

    int calculateRedeemableStars(int totalStars);

    int calculateRemainingStars(int totalStars);

    RewardRedemptionResponse redeem(UUID studentId, String idempotencyKey, RewardRedemptionRequest request);
}

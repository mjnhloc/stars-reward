package com.sr.reward.service;

import java.util.UUID;

import com.sr.reward.dto.request.RewardRedemptionRequest;
import com.sr.reward.dto.response.RewardRedemptionResponse;

public interface RewardRedemptionService {

    RewardRedemptionResponse redeem(UUID studentId, String idempotencyKey, RewardRedemptionRequest request);
}

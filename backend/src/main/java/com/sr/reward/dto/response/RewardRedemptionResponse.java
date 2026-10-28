package com.sr.reward.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RewardRedemptionResponse(
    UUID id,
    UUID studentId,
    int redeemableStars,
    long redeemableValue,
    int remainingStars,
    String status,
    OffsetDateTime paidAt
) {}

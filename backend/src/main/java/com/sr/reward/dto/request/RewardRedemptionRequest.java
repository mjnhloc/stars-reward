package com.sr.reward.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record RewardRedemptionRequest(
    @NotNull UUID classId
) {}

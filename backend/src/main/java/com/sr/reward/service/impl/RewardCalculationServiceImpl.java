package com.sr.reward.service.impl;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sr.reward.service.RewardCalculationService;

@Service
public class RewardCalculationServiceImpl implements RewardCalculationService {

    private final int starsPerRedemption;
    private final int vndPerRedemption;

    public RewardCalculationServiceImpl(
        @Value("${app.rewards.stars-per-redemption}")
        int starsPerRedemption,
        @Value("${app.rewards.vnd-per-redemption}")
        int vndPerRedemption
    ) {
        this.starsPerRedemption = starsPerRedemption;
        this.vndPerRedemption = vndPerRedemption;
    }

    @Override
    public long calculateRedeemableValue(int totalStars) {
        validateTotalStars(totalStars);

        long redemptionCount = totalStars / starsPerRedemption;

        return Math.multiplyExact(redemptionCount, vndPerRedemption);
    }

    @Override
    public int calculateRedeemableStars(int totalStars) {
        validateTotalStars(totalStars);

        return Math.multiplyExact(totalStars / starsPerRedemption, starsPerRedemption);
    }

    @Override
    public int calculateRemainingStars(int totalStars) {
        validateTotalStars(totalStars);

        return totalStars % starsPerRedemption;
    }

    @Override
    public int getStarsPerRedemption() {
        return starsPerRedemption;
    }

    @Override
    public long getVndPerRedemption() {
        return vndPerRedemption;
    }

    private void validateTotalStars(int totalStars) {
        if (totalStars < 0) {
            throw new IllegalArgumentException("Tổng số sao không được nhỏ hơn 0");
        }
    }
}

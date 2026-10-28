package com.sr.reward.service;

public interface RewardCalculationService {

    long calculateRedeemableValue(int totalStars);

    int calculateRedeemableStars(int totalStars);

    int calculateRemainingStars(int totalStars);

    int getStarsPerRedemption();

    long getVndPerRedemption();
}

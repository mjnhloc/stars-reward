export type RewardRedemptionRequest = {
    classId: string;
};

export type RewardRedemptionResponse = {
    redemptionId: string;
    studentId: string;
    starsRedeemed: number;
    amountVnd: number;
    remainingStars: number;
    status: "PENDING" | "PAID" | "CANCELLED";
    createdAt: string;
};

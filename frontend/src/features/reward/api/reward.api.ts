import { apiClient } from "../../../lib/apiClient";

import type {
    RewardRedemptionRequest,
    RewardRedemptionResponse,
} from "../types/reward.types";

export async function redeemStudentReward(
    studentId: string,
    request: RewardRedemptionRequest,
): Promise<RewardRedemptionResponse> {
    const response = await apiClient.post<RewardRedemptionResponse>(
        `/reward/${studentId}/redemptions`,
        request,
        {
            headers: {
                "Idempotency-Key": crypto.randomUUID(),
            },
        },
    );

    return response.data;
}

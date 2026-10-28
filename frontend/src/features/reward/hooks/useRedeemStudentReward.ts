import { useMutation, useQueryClient } from "@tanstack/react-query";

import { redeemStudentReward } from "../api/reward.api";

type RedeemStudentRewardVariables = {
    studentId: string;
    classId: string;
};

export function useRedeemStudentReward() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: ({ studentId, classId }: RedeemStudentRewardVariables) =>
            redeemStudentReward(studentId, {
                classId,
            }),

        onSuccess: (_, variables) => {
            queryClient.invalidateQueries({
                queryKey: ["students"],
            });

            queryClient.invalidateQueries({
                queryKey: ["dashboard"],
            });

            queryClient.invalidateQueries({
                queryKey: ["classes"],
            });

            queryClient.invalidateQueries({
                queryKey: ["student-transactions", variables.studentId],
            });

            queryClient.invalidateQueries({
                queryKey: ["student-redemptions", variables.studentId],
            });
        },
    });
}

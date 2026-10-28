import { useMutation, useQueryClient } from "@tanstack/react-query";

import { adjustStudentStars } from "../api/students.api";

type AdjustStudentStarsVariables = {
    studentId: string;
    classId: string;
    stars: number;
    reasonDetail: string;
};

export function useAdjustStudentStars() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: ({
            studentId,
            classId,
            stars,
            reasonDetail,
        }: AdjustStudentStarsVariables) =>
            adjustStudentStars(studentId, {
                classId,
                stars,
                reasonDetail,
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
        },
    });
}

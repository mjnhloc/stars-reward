import { useMutation, useQueryClient } from "@tanstack/react-query";

import { importScores } from "../api/score-import.api";

export function useImportScores() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: (file: File) => importScores(file),

        onSuccess: () => {
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
                queryKey: ["student-scores"],
            });

            queryClient.invalidateQueries({
                queryKey: ["student-transactions"],
            });

            queryClient.invalidateQueries({
                queryKey: ["score-imports"],
            });
        },
    });
}

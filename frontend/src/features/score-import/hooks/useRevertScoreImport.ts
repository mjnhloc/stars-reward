import { useMutation, useQueryClient } from "@tanstack/react-query";

import { revertScoreImport } from "../api/score-import.api";

export function useRevertScoreImport() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: (batchId: string) => revertScoreImport(batchId),

        onSuccess: () => {
            queryClient.invalidateQueries({
                queryKey: ["score-imports"],
            });

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
        },
    });
}

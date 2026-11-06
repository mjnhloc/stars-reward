import { useQuery } from "@tanstack/react-query";

import { getRevertPreview } from "../api/score-import.api";

export function useRevertPreview(batchId?: string) {
    return useQuery({
        queryKey: ["score-imports", batchId, "revert-preview"],
        queryFn: () => getRevertPreview(batchId!),
        enabled: Boolean(batchId),
        // balances change often, always load fresh numbers
        staleTime: 0,
        gcTime: 0,
    });
}

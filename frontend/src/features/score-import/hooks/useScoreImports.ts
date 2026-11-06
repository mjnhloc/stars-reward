import { useQuery } from "@tanstack/react-query";

import { getScoreImports } from "../api/score-import.api";

export function useScoreImports() {
    return useQuery({
        queryKey: ["score-imports"],
        queryFn: getScoreImports,
    });
}

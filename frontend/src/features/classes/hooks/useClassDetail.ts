import { useQuery } from "@tanstack/react-query";

import { getClassDetail } from "../api/classes.api";

export function useClassDetail(classId: string) {
    return useQuery({
        queryKey: ["classes", "detail", classId],
        queryFn: () => getClassDetail(classId),
    });
}

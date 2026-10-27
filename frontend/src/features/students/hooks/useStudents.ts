import { useQuery } from "@tanstack/react-query";

import { getStudents } from "../api/students.api";

export function useStudents(classId?: string) {
    return useQuery({
        queryKey: ["students", classId ?? "all"],
        queryFn: () => getStudents(classId),
    });
}

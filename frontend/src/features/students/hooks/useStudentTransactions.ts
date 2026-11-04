import { useQuery } from "@tanstack/react-query";

import { getStudentTransactions } from "../api/students.api";

export function useStudentTransactions(studentId?: string, enabled = true) {
    return useQuery({
        queryKey: ["student-transactions", studentId],
        queryFn: () => getStudentTransactions(studentId!),
        enabled: Boolean(studentId) && enabled,
    });
}

import { useQuery } from "@tanstack/react-query";

import { getStudentScores } from "../api/students.api";

export function useStudentScores(studentId?: string) {
    return useQuery({
        queryKey: ["student-scores", studentId],
        queryFn: () => getStudentScores(studentId!),
        enabled: Boolean(studentId),
    });
}

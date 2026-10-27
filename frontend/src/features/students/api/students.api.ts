import { apiClient } from "../../../lib/apiClient";

import type { Student, StudentScore } from "../types/student.types";

export async function getStudents(classId?: string): Promise<Student[]> {
    const response = await apiClient.get<Student[]>("/students", {
        params: {
            classId: classId || undefined,
        },
    });

    return response.data;
}

export async function getStudentScores(
    studentId: string,
): Promise<StudentScore[]> {
    const response = await apiClient.get<StudentScore[]>(
        `/students/${studentId}/scores`,
    );

    return response.data;
}

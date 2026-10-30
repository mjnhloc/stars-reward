import { apiClient } from "../../../lib/apiClient";

import type {
    CreateStudentRequest,
    CreateStudentResponse,
    Student,
    StudentScore,
    StudentStarsAdjustmentRequest,
    StudentStarsAdjustmentResponse,
} from "../types/student.types";

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

export async function adjustStudentStars(
    studentId: string,
    request: StudentStarsAdjustmentRequest,
): Promise<StudentStarsAdjustmentResponse> {
    const response = await apiClient.post<StudentStarsAdjustmentResponse>(
        `/students/${studentId}/stars/adjustments`,
        request,
        {
            headers: {
                "Idempotency-Key": crypto.randomUUID(),
            },
        },
    );

    return response.data;
}

export async function createStudent(
    request: CreateStudentRequest,
): Promise<CreateStudentResponse> {
    const response = await apiClient.post<CreateStudentResponse>(
        "/students",
        request,
    );

    return response.data;
}

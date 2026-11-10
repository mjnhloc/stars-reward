import { apiClient } from "../../../lib/apiClient";

import type { StudentTransaction } from "../../star-transactions/types/star-transaction.types";
import type {
    CreateStudentRequest,
    CreateStudentResponse,
    RestoreStudentRequest,
    Student,
    StudentScore,
    StudentStarsAdjustmentRequest,
    StudentStarsAdjustmentResponse,
    StudentTrash,
    UpdateStudentRequest,
    UpdateStudentResponse,
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

export async function getStudentTransactions(
    studentId: string,
): Promise<StudentTransaction[]> {
    const response = await apiClient.get<StudentTransaction[]>(
        `/students/${studentId}/transactions`,
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

export async function updateStudent(
    studentId: string,
    request: UpdateStudentRequest,
): Promise<UpdateStudentResponse> {
    const response = await apiClient.put<UpdateStudentResponse>(
        `/students/${studentId}`,
        request,
    );

    return response.data;
}

export async function deleteStudent(studentId: string): Promise<void> {
    await apiClient.delete(`/students/${studentId}`);
}

export async function getStudentTrash(): Promise<StudentTrash[]> {
    const response = await apiClient.get<StudentTrash[]>("/students/trash");

    return response.data;
}

export async function restoreStudent(
    studentId: string,
    request: RestoreStudentRequest,
): Promise<UpdateStudentResponse> {
    const response = await apiClient.post<UpdateStudentResponse>(
        `/students/${studentId}/restore`,
        request,
    );

    return response.data;
}

export async function deleteStudentPermanently(
    studentId: string,
): Promise<void> {
    await apiClient.delete(`/students/${studentId}/permanent`);
}

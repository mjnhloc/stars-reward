import { apiClient } from "../../../lib/apiClient";
import {
    ClassDetail,
    ClassSummary,
    ClassTrash,
    CreateClassRequest,
    CreateClassResponse,
    UpdateClassRequest,
} from "../types/class.types";

export async function getClasses(): Promise<ClassSummary[]> {
    const response = await apiClient.get<ClassSummary[]>("/classes");

    return response.data;
}

export async function createClass(
    request: CreateClassRequest,
): Promise<CreateClassResponse> {
    const response = await apiClient.post<CreateClassResponse>(
        "/classes",
        request,
    );

    return response.data;
}

export async function getClassDetail(classId: string): Promise<ClassDetail> {
    const response = await apiClient.get<ClassDetail>(`/classes/${classId}`);

    return response.data;
}

export async function updateClass(
    classId: string,
    request: UpdateClassRequest,
): Promise<ClassDetail> {
    const response = await apiClient.put<ClassDetail>(
        `/classes/${classId}`,
        request,
    );

    return response.data;
}

export async function deleteClass(classId: string): Promise<void> {
    await apiClient.delete(`/classes/${classId}`);
}

export async function getClassTrash(): Promise<ClassTrash[]> {
    const response = await apiClient.get<ClassTrash[]>("/classes/trash");

    return response.data;
}

export async function restoreClass(classId: string): Promise<ClassDetail> {
    const response = await apiClient.post<ClassDetail>(
        `/classes/${classId}/restore`,
    );

    return response.data;
}

export async function deleteClassPermanently(classId: string): Promise<void> {
    await apiClient.delete(`/classes/${classId}/permanent`);
}

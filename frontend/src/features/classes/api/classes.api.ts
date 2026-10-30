import { apiClient } from "../../../lib/apiClient";
import {
    ClassSummary,
    CreateClassRequest,
    CreateClassResponse,
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

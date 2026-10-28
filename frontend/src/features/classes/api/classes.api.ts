import { apiClient } from "../../../lib/apiClient";
import { ClassSummary } from "../types/class.types";

export async function getClasses(): Promise<ClassSummary[]> {
    const response = await apiClient.get<ClassSummary[]>("/classes");

    return response.data;
}

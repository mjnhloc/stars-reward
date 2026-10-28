import { apiClient } from "../../../lib/apiClient";
import type { Dashboard } from "../types/dashboard.types";

export async function getDashboard(): Promise<Dashboard> {
    const response = await apiClient.get<Dashboard>("/dashboard");

    return response.data;
}

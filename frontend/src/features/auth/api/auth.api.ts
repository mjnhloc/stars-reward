import { apiClient } from "../../../lib/apiClient";
import type {
    AuthUser,
    LoginRequest,
    LoginResponse,
} from "../types/auth.types"

export async function login(request: LoginRequest): Promise<LoginResponse> {
    const response = await apiClient.post<LoginResponse>("/auth/login", request);

    return response.data;
}

export async function getCurrentUser(): Promise<AuthUser> {
    const response = await apiClient.get<AuthUser>("/auth/me");

    return response.data;
}

export async function logout(): Promise<void> {
    await apiClient.post("/auth/logout");
}
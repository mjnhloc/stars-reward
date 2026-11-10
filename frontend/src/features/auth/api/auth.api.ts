import { apiClient } from "../../../lib/apiClient";
import type {
    AuthUser,
    ChangePasswordResponse,
    LoginRequest,
    LoginResponse,
    TokenResponse,
} from "../types/auth.types";

import { ChangePasswordRequest } from "../types/auth.types";

export async function login(request: LoginRequest): Promise<LoginResponse> {
    const response = await apiClient.post<LoginResponse>(
        "/auth/login",
        request,
    );

    return response.data;
}

export async function getCurrentUser(): Promise<AuthUser> {
    const response = await apiClient.get<AuthUser>("/auth/me");

    return response.data;
}

export async function logoutRequest(): Promise<void> {
    await apiClient.post("/auth/logout");
}

export async function changePassword(
    request: ChangePasswordRequest,
): Promise<ChangePasswordResponse> {
    const response = await apiClient.put<ChangePasswordResponse>(
        "/auth/change-password",
        request,
    );

    return response.data;
}

export async function refreshAccessToken(): Promise<TokenResponse> {
    const response = await apiClient.post<TokenResponse>("/auth/refresh");

    return response.data;
}

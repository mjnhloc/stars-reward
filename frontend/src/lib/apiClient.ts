import axios from "axios";
import { refreshAccessToken } from "../features/auth/api/auth.api";
import { tokenStorage } from "../features/auth/services/tokenStorage";
import { toApiError } from "./apiError";
import type { TokenResponse } from "../features/auth/types/auth.types";

let refreshPromise: Promise<TokenResponse> | null = null;

export const apiClient = axios.create({
    baseURL: import.meta.env.VITE_API_URL,
    timeout: 15000,
    headers: {
        Accept: "application/json",
        "Content-Type": "application/json",
    },
    withCredentials: true,
});

apiClient.interceptors.request.use((config) => {
    const accessToken = tokenStorage.getFromAnyStorage();

    if (accessToken) {
        config.headers.Authorization = `Bearer ${accessToken}`;
    }

    return config;
});

apiClient.interceptors.response.use(
    (response) => response,

    async (error) => {
        const originalRequest = error.config;

        const isUnauthorized = error.response?.status === 401;

        const isAuthRequest =
            originalRequest?.url?.includes("/auth/login") ||
            originalRequest?.url?.includes("/auth/refresh");

        if (isUnauthorized && !isAuthRequest && !originalRequest._retry) {
            originalRequest._retry = true;

            try {
                refreshPromise ??= refreshAccessToken().finally(() => {
                    refreshPromise = null;
                });

                const refreshed = await refreshPromise;

                // keep the token where the remember me choice of the session wants it
                tokenStorage.set(refreshed.accessToken, refreshed.remember);

                originalRequest.headers.Authorization = `Bearer ${refreshed.accessToken}`;

                return apiClient(originalRequest);
            } catch {
                tokenStorage.clear();
                window.location.replace("/login");

                return Promise.reject(toApiError(error));
            }
        }

        return Promise.reject(toApiError(error));
    },
);

import axios from "axios";
import { tokenStorage } from "../features/auth/services/tokenStorage";
import { TokenResponse } from "../features/auth/types/auth.types";
import { toApiError } from "./apiError";

let refreshPromise: Promise<string> | null = null;

async function refreshAccessToken(): Promise<string> {
    const response = await apiClient.post<TokenResponse>("/auth/refresh");

    tokenStorage.set(response.data.accessToken, false);

    return response.data.accessToken;
}

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

                const newAccessToken = await refreshPromise;

                originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;

                return apiClient(originalRequest);
            } catch {
                tokenStorage.clear();
                window.location.replace("/login");

                return Promise.reject(error);
            }
        }

        return Promise.reject(toApiError(error));
    },
);

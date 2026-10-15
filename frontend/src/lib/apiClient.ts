import axios from "axios";
import { tokenStorage } from "../features/auth/services/tokenStorage";
import { toApiError } from "./apiError";

export const apiClient = axios.create({
    baseURL: import.meta.env.VITE_API_URL,
    timeout: 15000,
    headers: {
        Accept: "application/json",
        "Content-Type": "application/json",
    },
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
    (error: unknown) => {
        const apiError = toApiError(error);

        const requestUrl = axios.isAxiosError(error)
            ? error.config?.url
            : undefined;

        const isLoginRequest = requestUrl?.includes("/auth/login") === true;

        if (apiError.status === 401 && !isLoginRequest) {
            tokenStorage.clear();

            if (window.location.pathname !== "/login") {
                window.location.replace("/login");
            }
        }

        return Promise.reject(apiError);
    },
);

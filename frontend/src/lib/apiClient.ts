import axios from "axios";
import { tokenStorage } from "../features/auth/services/tokenStorage";

export const apiClient = axios.create({
    baseURL: import.meta.env.VITE_API_URL ?? "http://localhost:8080/api/v1",
    timeout: 15000,
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
    (error) => {
        if (error.response?.status == 401) {
            tokenStorage.clear();

            if (window.location.pathname !== "/login") {
                window.location.replace("/login");
            }
        }

        const message = error.response?.data?.message ?? "Không thể kết nối máy chủ";
        
        return Promise.reject(new Error(message));
    } 
);

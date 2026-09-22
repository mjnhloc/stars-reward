import axios from "axios";

export const api = axios.create({
    baseURL: import.meta.env.VITE_API_URL ?? "http://localhost:8080/api/v1",
    timeout: 15000,
});

api.interceptors.request.use((config) => {
    const token = sessionStorage.getItem("access_token");
    if (token) config.headers.Authorization = `Bearer ${token}`;
    return config;
});

api.interceptors.response.use(
    (r) => r,
    (e) =>
        Promise.reject(
            new Error(e.response?.data?.message ?? "Không thể kết nối máy chủ"),
        ),
);

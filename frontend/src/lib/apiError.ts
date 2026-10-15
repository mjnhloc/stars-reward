import axios from "axios";

export type ApiErrorBody = {
    status?: number;
    code?: string;
    message?: string;
    path?: string;
    timestamp?: string;
};

export class ApiError extends Error {
    readonly status?: number;
    readonly code?: string;

    constructor(message: string, status?: number, code?: string) {
        super(message);

        this.name = "ApiError";
        this.status = status;
        this.code = code;
    }
}

export function toApiError(error: unknown): ApiError {
    if (!axios.isAxiosError<ApiErrorBody>(error)) {
        return new ApiError(
            error instanceof Error
                ? error.message
                : "Undefined error happened.",
        );
    }

    // backend does response
    if (error.response) {
        const body = error.response.data;

        return new ApiError(
            body?.message ?? "Request cannot be processed.",
            error.response.status,
            body.code,
        );
    }

    // backend does not response
    if (error.request) {
        return new ApiError(
            "Cannot connect to the server.",
            undefined,
            "NETWORK_ERROR",
        );
    }

    return new ApiError(
        "Cannot create request to the server.",
        undefined,
        "REQUEST_ERROR",
    );
}

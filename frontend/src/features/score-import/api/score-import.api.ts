import { apiClient } from "../../../lib/apiClient";

import type {
    ScoreImportPreview,
    ScoreImportResult,
} from "../types/score-import.types";

// file upload can take longer than normal requests
const UPLOAD_TIMEOUT = 60000;

function toFormData(file: File): FormData {
    const formData = new FormData();

    formData.append("file", file);

    return formData;
}

export async function previewScoreImport(
    file: File,
): Promise<ScoreImportPreview> {
    const response = await apiClient.post<ScoreImportPreview>(
        "/score-imports/preview",
        toFormData(file),
        {
            // apiClient sends JSON by default, axios would convert FormData to JSON
            headers: { "Content-Type": "multipart/form-data" },
            timeout: UPLOAD_TIMEOUT,
        },
    );

    return response.data;
}

export async function importScores(file: File): Promise<ScoreImportResult> {
    const response = await apiClient.post<ScoreImportResult>(
        "/score-imports",
        toFormData(file),
        {
            headers: { "Content-Type": "multipart/form-data" },
            timeout: UPLOAD_TIMEOUT,
        },
    );

    return response.data;
}

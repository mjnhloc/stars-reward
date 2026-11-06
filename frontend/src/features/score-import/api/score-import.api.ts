import { apiClient } from "../../../lib/apiClient";

import type {
    ImportBatch,
    ImportRevertPreview,
    ScoreImportPreview,
    ScoreImportResult,
    ScoreImportRevertResult,
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

export async function getScoreImports(): Promise<ImportBatch[]> {
    const response = await apiClient.get<ImportBatch[]>("/score-imports");

    return response.data;
}

export async function getRevertPreview(
    batchId: string,
): Promise<ImportRevertPreview> {
    const response = await apiClient.get<ImportRevertPreview>(
        `/score-imports/${batchId}/revert-preview`,
    );

    return response.data;
}

export async function revertScoreImport(
    batchId: string,
): Promise<ScoreImportRevertResult> {
    const response = await apiClient.post<ScoreImportRevertResult>(
        `/score-imports/${batchId}/revert`,
    );

    return response.data;
}

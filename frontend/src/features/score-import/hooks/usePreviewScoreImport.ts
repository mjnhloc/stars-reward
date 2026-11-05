import { useMutation } from "@tanstack/react-query";

import { previewScoreImport } from "../api/score-import.api";

export function usePreviewScoreImport() {
    return useMutation({
        mutationFn: (file: File) => previewScoreImport(file),
    });
}

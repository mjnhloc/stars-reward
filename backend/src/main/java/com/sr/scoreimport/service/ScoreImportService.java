package com.sr.scoreimport.service;

import java.util.List;
import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import com.sr.scoreimport.dto.response.ImportBatchResponse;
import com.sr.scoreimport.dto.response.ImportRevertPreviewResponse;
import com.sr.scoreimport.dto.response.ScoreImportPreviewResponse;
import com.sr.scoreimport.dto.response.ScoreImportResultResponse;
import com.sr.scoreimport.dto.response.ScoreImportRevertResponse;

public interface ScoreImportService {

    ScoreImportPreviewResponse preview(MultipartFile file);

    ScoreImportResultResponse importScores(MultipartFile file);

    List<ImportBatchResponse> getImports();

    ImportRevertPreviewResponse getRevertPreview(UUID batchId);

    ScoreImportRevertResponse revertImport(UUID batchId);
}

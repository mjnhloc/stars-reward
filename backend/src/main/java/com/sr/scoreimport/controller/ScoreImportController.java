package com.sr.scoreimport.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.sr.scoreimport.dto.response.ImportBatchResponse;
import com.sr.scoreimport.dto.response.ImportRevertPreviewResponse;
import com.sr.scoreimport.dto.response.ScoreImportPreviewResponse;
import com.sr.scoreimport.dto.response.ScoreImportResultResponse;
import com.sr.scoreimport.dto.response.ScoreImportRevertResponse;
import com.sr.scoreimport.service.ScoreImportService;

@RestController
@RequestMapping("/api/v1/score-imports")
public class ScoreImportController {

    private final ScoreImportService scoreImportService;

    public ScoreImportController(ScoreImportService scoreImportService) {
        this.scoreImportService = scoreImportService;
    }

    /*
     * Read the file and show what will be imported, nothing is saved
     */
    @PostMapping(value = "/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ScoreImportPreviewResponse preview(
        @RequestParam("file")
        MultipartFile file
    ) {
        return scoreImportService.preview(file);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ScoreImportResultResponse importScores(
        @RequestParam("file")
        MultipartFile file,
        @RequestParam(value = "createStudentCodes", required = false)
        List<String> createStudentCodes
    ) {
        return scoreImportService.importScores(file, createStudentCodes);
    }

    @GetMapping
    public List<ImportBatchResponse> getImports() {
        return scoreImportService.getImports();
    }

    /*
     * Show which students lose how many stars, nothing is saved
     */
    @GetMapping("/{batchId}/revert-preview")
    public ImportRevertPreviewResponse getRevertPreview(
        @PathVariable
        UUID batchId
    ) {
        return scoreImportService.getRevertPreview(batchId);
    }

    @PostMapping("/{batchId}/revert")
    public ScoreImportRevertResponse revertImport(
        @PathVariable
        UUID batchId
    ) {
        return scoreImportService.revertImport(batchId);
    }
}

package com.sr.scoreimport.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.sr.scoreimport.dto.response.ScoreImportPreviewResponse;
import com.sr.scoreimport.dto.response.ScoreImportResultResponse;
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
        MultipartFile file
    ) {
        return scoreImportService.importScores(file);
    }
}

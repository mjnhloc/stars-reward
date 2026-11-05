package com.sr.scoreimport.service;

import org.springframework.web.multipart.MultipartFile;

import com.sr.scoreimport.dto.response.ScoreImportPreviewResponse;
import com.sr.scoreimport.dto.response.ScoreImportResultResponse;

public interface ScoreImportService {

    ScoreImportPreviewResponse preview(MultipartFile file);

    ScoreImportResultResponse importScores(MultipartFile file);
}

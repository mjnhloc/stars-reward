package com.sr.scoreimport.dto.response;

import java.util.List;

public record ScoreImportPreviewResponse(
    String fileName,
    String sheetName,
    List<ImportClassResponse> classes,
    List<ImportStudentResponse> students,
    List<ImportNewStudentResponse> newStudents,
    ImportTotalsResponse totals,
    List<ImportIssueResponse> issues
) {}

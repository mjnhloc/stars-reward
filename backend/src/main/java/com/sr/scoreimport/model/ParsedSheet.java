package com.sr.scoreimport.model;

import java.util.List;

import com.sr.scoreimport.dto.response.ImportIssueResponse;

public record ParsedSheet(
    String sheetName,
    List<ParsedClassBlock> blocks,
    List<ImportIssueResponse> issues
) {}

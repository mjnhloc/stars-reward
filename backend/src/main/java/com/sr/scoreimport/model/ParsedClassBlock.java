package com.sr.scoreimport.model;

import java.util.List;

public record ParsedClassBlock(
    String classCode,
    int headerRowNumber,
    List<ParsedStudent> students
) {}

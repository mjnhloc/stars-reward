package com.sr.scoreimport.model;

import java.util.List;

public record ParsedStudent(
    int rowNumber,
    String studentCode,
    String fullName,
    List<ParsedSession> sessions
) {}

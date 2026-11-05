package com.sr.scoreimport.dto.response;

import java.util.List;

public record ImportStudentResponse(
    String studentCode,
    String fullName,
    String classCode,
    int newSessions,
    int skippedSessions,
    int starsToAward,
    List<ImportSessionResponse> sessions
) {}

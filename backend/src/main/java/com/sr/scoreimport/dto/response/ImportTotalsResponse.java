package com.sr.scoreimport.dto.response;

public record ImportTotalsResponse(
    int students,
    int newSessions,
    int skippedSessions,
    int starsToAward,
    int errors
) {}

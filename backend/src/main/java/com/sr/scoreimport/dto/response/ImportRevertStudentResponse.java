package com.sr.scoreimport.dto.response;

public record ImportRevertStudentResponse(
    String studentCode,
    String fullName,
    int starsToReverse,
    int currentBalance,
    boolean enoughBalance
) {}

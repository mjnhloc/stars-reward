package com.sr.student.dto.response;

import java.util.UUID;

public record StudentStarsAdjustmentResponse(
    UUID transactionId, 
    UUID studentId, 
    int delta, 
    int balanceAfter
) {}

package com.sr.scoreimport.dto.response;

import java.util.UUID;

public record ImportBatchClass(
    UUID batchId,
    String className
) {}

package com.sr.common.audit.dto;

import java.util.Map;
import java.util.UUID;

public record AuditLogCommand(
    UUID actorId,
    String action,
    String entityType,
    UUID entityId,
    Map<String, Object> details
) {}
package com.sr.common.audit.service;

import com.sr.common.audit.dto.AuditLogCommand;

public interface AuditLogService {
    void record(AuditLogCommand command);
}

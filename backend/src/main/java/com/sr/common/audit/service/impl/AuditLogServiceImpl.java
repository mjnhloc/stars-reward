package com.sr.common.audit.service.impl;

import org.springframework.stereotype.Service;

import com.sr.common.audit.dto.AuditLogCommand;
import com.sr.common.audit.entity.AuditLogEntity;
import com.sr.common.audit.repository.AuditLogRepository;
import com.sr.common.audit.service.AuditLogService;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Service
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final JsonMapper jsonMapper;

    public AuditLogServiceImpl(
        AuditLogRepository auditLogRepository,
        JsonMapper jsonMapper
    ) {
        this.auditLogRepository = auditLogRepository;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void record(AuditLogCommand command) {
        validate(command);

        String detailsJson = serializeDetails(command);

        auditLogRepository.save(
            AuditLogEntity.create(
                command.actorId(),
                command.action(),
                command.entityType(),
                command.entityId(),
                detailsJson
            )
        );
    }

    private String serializeDetails(AuditLogCommand command) {
        try {
            return jsonMapper.writeValueAsString(
                command.details()
            );
        } catch (JacksonException exception) {
            throw new IllegalStateException(
                "Không thể chuyển audit details thành JSON",
                exception
            );
        }
    }

    private void validate(AuditLogCommand command) {
        if (command.actorId() == null) {
            throw new IllegalArgumentException(
                "Audit actorId không được để trống"
            );
        }

        if (command.action() == null || command.action().isBlank()) {
            throw new IllegalArgumentException(
                "Audit action không được để trống"
            );
        }

        if (command.entityType() == null || command.entityType().isBlank()) {
            throw new IllegalArgumentException(
                "Audit entityType không được để trống"
            );
        }

        if (command.entityId() == null) {
            throw new IllegalArgumentException(
                "Audit entityId không được để trống"
            );
        }
    }
}
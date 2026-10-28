package com.sr.common.audit.service.impl;

import java.util.UUID;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import com.sr.common.audit.dto.AuditLogCommand;
import com.sr.common.audit.service.AuditLogService;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Service
public class AuditLogServiceImpl implements AuditLogService {

    private final JdbcClient jdbcClient;
    private final JsonMapper jsonMapper;

    public AuditLogServiceImpl(
        JdbcClient jdbcClient,
        JsonMapper jsonMapper
    ) {
        this.jdbcClient = jdbcClient;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void record(AuditLogCommand command) {
        validate(command);

        String detailsJson = serializeDetails(command);

        int insertedRows = jdbcClient.sql("""
            INSERT INTO audit_logs (
                id,
                actor_id,
                action,
                entity_type,
                entity_id,
                details
            )
            VALUES (
                :id,
                :actorId,
                :action,
                :entityType,
                :entityId,
                CAST(:details AS JSONB)
            )
            """)
            .param("id", UUID.randomUUID())
            .param("actorId", command.actorId())
            .param("action", command.action())
            .param("entityType", command.entityType())
            .param("entityId", command.entityId())
            .param("details", detailsJson)
            .update();

        if (insertedRows != 1) {
            throw new IllegalStateException(
                "Không thể ghi audit log"
            );
        }
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
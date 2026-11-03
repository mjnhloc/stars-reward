package com.sr.common.audit.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Immutable
@Table(name = "audit_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(nullable = false)
    private String action;

    @Column(name = "entity_type", nullable = false)
    private String entityType;

    @Column(name = "entity_id")
    private UUID entityId;

    /*
     * Already serialized JSON text, stored into the jsonb column as is
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private String details;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public static AuditLogEntity create(
        UUID actorId,
        String action,
        String entityType,
        UUID entityId,
        String details
    ) {
        AuditLogEntity auditLog = new AuditLogEntity();
        auditLog.actorId = actorId;
        auditLog.action = action;
        auditLog.entityType = entityType;
        auditLog.entityId = entityId;
        auditLog.details = details;
        return auditLog;
    }
}

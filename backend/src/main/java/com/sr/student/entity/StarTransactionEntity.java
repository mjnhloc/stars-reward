package com.sr.student.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/*
 * Ledger rows are immutable: Hibernate only inserts, never updates them.
 * Corrections must use reversal transactions
 */
@Entity
@Immutable
@Table(name = "star_transactions")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class StarTransactionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "class_id", nullable = false)
    private UUID classId;

    @Column(name = "redemption_id")
    private UUID redemptionId;

    @Column(name = "transaction_type", nullable = false)
    private String transactionType;

    @Column(name = "source_type", nullable = false)
    private String sourceType;

    @Column(name = "star_delta", nullable = false)
    private int starDelta;

    @Column(name = "balance_before", nullable = false)
    private int balanceBefore;

    @Column(name = "balance_after", nullable = false)
    private int balanceAfter;

    @Column(name = "reason_detail", nullable = false)
    private String reasonDetail;

    @Column(name = "score_id")
    private UUID scoreId;

    @Column(name = "performed_by", nullable = false)
    private UUID performedBy;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;

    @Column(name = "reversed_transaction_id")
    private UUID reversedTransactionId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}

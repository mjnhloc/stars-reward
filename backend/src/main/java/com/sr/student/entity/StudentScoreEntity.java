package com.sr.student.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.Immutable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
 * Read only in this application, scores are written by the import process
 */
@Entity
@Immutable
@Table(name = "student_scores")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudentScoreEntity {

    @Id
    private UUID id;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "class_id", nullable = false)
    private UUID classId;

    @Column(name = "homework_score", nullable = false)
    private BigDecimal homeworkScore;

    @Column(name = "test_score", nullable = false)
    private BigDecimal testScore;

    @Column(name = "final_score", nullable = false)
    private BigDecimal finalScore;

    @Column(name = "awarded_stars", nullable = false)
    private int awardedStars;

    @Column(name = "assessment_date", nullable = false)
    private LocalDate assessmentDate;

    @Column(name = "import_batch_id")
    private UUID importBatchId;

    @Column(name = "external_row_id")
    private String externalRowId;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}

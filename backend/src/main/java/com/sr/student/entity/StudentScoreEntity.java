package com.sr.student.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
 * Scores are written by the score import process. Score values never change,
 * a reverted import only marks its scores as voided
 */
@Entity
@Table(name = "student_scores")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudentScoreEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
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

    @Column(name = "voided_at")
    private OffsetDateTime voidedAt;

    @Column(name = "voided_by")
    private UUID voidedBy;

    public static StudentScoreEntity create(
        UUID studentId,
        UUID classId,
        BigDecimal homeworkScore,
        BigDecimal testScore,
        BigDecimal finalScore,
        int awardedStars,
        LocalDate assessmentDate,
        UUID importBatchId,
        String externalRowId
    ) {
        StudentScoreEntity score = new StudentScoreEntity();
        score.studentId = studentId;
        score.classId = classId;
        score.homeworkScore = homeworkScore;
        score.testScore = testScore;
        score.finalScore = finalScore;
        score.awardedStars = awardedStars;
        score.assessmentDate = assessmentDate;
        score.importBatchId = importBatchId;
        score.externalRowId = externalRowId;
        score.createdAt = OffsetDateTime.now();
        return score;
    }

    public void voidBy(UUID actorId) {
        this.voidedAt = OffsetDateTime.now();
        this.voidedBy = actorId;
    }
}

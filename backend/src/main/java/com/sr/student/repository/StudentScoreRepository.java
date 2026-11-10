package com.sr.student.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sr.scoreimport.dto.response.ImportBatchClass;
import com.sr.scoreimport.dto.response.ImportBatchSummary;
import com.sr.student.dto.response.StudentScoreResponse;
import com.sr.student.entity.StudentScoreEntity;

public interface StudentScoreRepository extends JpaRepository<StudentScoreEntity, UUID> {

    @Query("""
        SELECT new com.sr.student.dto.response.StudentScoreResponse(
            s.id,
            s.homeworkScore,
            s.testScore,
            s.finalScore,
            s.awardedStars,
            s.assessmentDate,
            s.createdAt
        )
        FROM StudentScoreEntity s
        WHERE s.studentId = :studentId
            AND s.voidedAt IS NULL
        ORDER BY
            s.assessmentDate DESC,
            s.createdAt DESC
        """)
    List<StudentScoreResponse> findScores(
        @Param("studentId")
        UUID studentId
    );

    List<StudentScoreEntity> findByStudentIdInAndVoidedAtIsNull(Collection<UUID> studentIds);

    List<StudentScoreEntity> findByImportBatchIdAndVoidedAtIsNull(UUID importBatchId);

    /*
     * Number of sessions, students and stars of every import batch. Voided scores
     * are counted too, so that a reverted import still shows what it imported
     */
    @Query("""
        SELECT new com.sr.scoreimport.dto.response.ImportBatchSummary(
            s.importBatchId,
            COUNT(s),
            COUNT(DISTINCT s.studentId),
            COALESCE(SUM(s.awardedStars), 0L)
        )
        FROM StudentScoreEntity s
        WHERE s.importBatchId IN :batchIds
        GROUP BY s.importBatchId
        """)
    List<ImportBatchSummary> summarizeBatches(@Param("batchIds") Collection<UUID> batchIds);

    /*
     * Classes of every import batch
     */
    @Query("""
        SELECT DISTINCT new com.sr.scoreimport.dto.response.ImportBatchClass(
            s.importBatchId,
            c.name
        )
        FROM StudentScoreEntity s
        JOIN ClassEntity c
            ON c.id = s.classId
        WHERE s.importBatchId IN :batchIds
        """)
    List<ImportBatchClass> findBatchClasses(@Param("batchIds") Collection<UUID> batchIds);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "DELETE FROM student_scores WHERE student_id = :studentId", nativeQuery = true)
    int deleteAllOfStudent(@Param("studentId") UUID studentId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "UPDATE student_scores SET class_id = NULL WHERE class_id = :classId", nativeQuery = true)
    int detachClass(@Param("classId") UUID classId);
}

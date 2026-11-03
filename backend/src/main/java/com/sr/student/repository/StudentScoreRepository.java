package com.sr.student.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
        ORDER BY
            s.assessmentDate DESC,
            s.createdAt DESC
        """)
    List<StudentScoreResponse> findScores(@Param("studentId") UUID studentId);
}

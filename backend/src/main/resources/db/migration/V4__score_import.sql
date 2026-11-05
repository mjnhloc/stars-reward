-- one score per student per session, so that re-uploading the same file never duplicates scores
CREATE UNIQUE INDEX uq_student_score_session
    ON student_scores (student_id, assessment_date);

ALTER TABLE student_scores
    ADD COLUMN voided_at TIMESTAMPTZ,
    ADD COLUMN voided_by uuid REFERENCES users;

-- the score of a reverted import does not count anymore, so the session can be imported again
DROP INDEX uq_student_score_session;

CREATE UNIQUE INDEX uq_student_score_session
    ON student_scores (student_id, assessment_date)
    WHERE voided_at IS NULL;

CREATE INDEX ix_student_scores_import_batch
    ON student_scores (import_batch_id);

ALTER TABLE import_batches
    ADD COLUMN reverted_at TIMESTAMPTZ,
    ADD COLUMN reverted_by uuid REFERENCES users;

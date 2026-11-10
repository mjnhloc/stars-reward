-- students who moved to another class keep their old score and star history
-- when that old class is permanently deleted, only the class name is lost
ALTER TABLE star_transactions ALTER COLUMN class_id DROP NOT NULL;

ALTER TABLE student_scores ALTER COLUMN class_id DROP NOT NULL;

-- Remove demo classes and students created by V2 (fixed ids). The admin user
-- '00000000-0000-0000-0000-000000000001' is kept because it is the only login account.
-- Rows that were already deleted by hand are simply skipped.
-- If real data still references a demo class (e.g. a real student moved into class 7A),
-- the foreign keys stop this migration instead of deleting real data.

DELETE FROM star_transactions
WHERE student_id IN (
    '00000000-0000-0000-0000-000000001001',
    '00000000-0000-0000-0000-000000001002',
    '00000000-0000-0000-0000-000000001003'
);

DELETE FROM reward_redemptions
WHERE student_id IN (
    '00000000-0000-0000-0000-000000001001',
    '00000000-0000-0000-0000-000000001002',
    '00000000-0000-0000-0000-000000001003'
);

DELETE FROM student_scores
WHERE student_id IN (
    '00000000-0000-0000-0000-000000001001',
    '00000000-0000-0000-0000-000000001002',
    '00000000-0000-0000-0000-000000001003'
);

DELETE FROM student_star_balances
WHERE student_id IN (
    '00000000-0000-0000-0000-000000001001',
    '00000000-0000-0000-0000-000000001002',
    '00000000-0000-0000-0000-000000001003'
);

DELETE FROM class_enrollments
WHERE student_id IN (
    '00000000-0000-0000-0000-000000001001',
    '00000000-0000-0000-0000-000000001002',
    '00000000-0000-0000-0000-000000001003'
);

DELETE FROM class_user_assignments
WHERE class_id IN (
    '00000000-0000-0000-0000-000000000101',
    '00000000-0000-0000-0000-000000000102'
);

DELETE FROM students
WHERE id IN (
    '00000000-0000-0000-0000-000000001001',
    '00000000-0000-0000-0000-000000001002',
    '00000000-0000-0000-0000-000000001003'
);

DELETE FROM classes
WHERE id IN (
    '00000000-0000-0000-0000-000000000101',
    '00000000-0000-0000-0000-000000000102'
);

# Google Sheets import design

Google Sheets is an import source, never the system of record. A scheduled or manual import creates an `import_batches` row, downloads values through a service account, validates all rows, stores row errors, and applies valid scores idempotently.

Recommended columns: `student_code`, `class_code`, `homework_score`, `test_score`, `assessment_date`, `external_row_id`, `updated_at`.

Use `(source_sheet_id, external_row_id, source_updated_at)` as a unique source identity. Credentials must come from a secret manager, not Git.

# API overview

Base path: `/api/v1`

- `GET /dashboard?classId=`
- `GET /classes`
- `GET /students?classId=&q=&page=&size=`
- `GET /students/{id}`
- `GET /students/{id}/transactions?page=&size=`
- `POST /students/{id}/stars/adjustments`
- `POST /score-imports/preview` (multipart `file`, Excel): read the score sheet and return what will be imported, nothing is saved
- `POST /score-imports` (multipart `file`, Excel): import new sessions and award stars. Already imported sessions (same student and date) are skipped

Mutation requests require `Idempotency-Key` header.

Example:
```json
{
  "classId": "00000000-0000-0000-0000-000000000101",
  "stars": -1,
  "reasonCode": "LATE_OVER_15_MINUTES",
  "reasonDetail": "Đến lớp lúc 07:48"
}
```

# API overview

Base path: `/api/v1`

- `POST /auth/refresh`: new access token from the refresh cookie, `remember` tells the frontend where to keep it
- `GET /dashboard?classId=`
- `GET /classes`
- `GET /classes/trash`: deleted classes, with the number of deleted students whose last class is this one (admin)
- `POST /classes/{id}/restore`: restore a deleted class (admin)
- `DELETE /classes/{id}/permanent`: permanently delete a class in the trash. Blocked while the trash still has students of this class. Students who moved to another class keep their old scores and stars without the class (admin)
- `GET /students?classId=&q=&page=&size=` (every student has `startedAt` of the current class)
- `GET /students/{id}`
- `PUT /students/{id}`: `startedAt` is required. Same class: corrects the start date. Another class: the student moves on `startedAt` (admin)
- `GET /students/{id}/transactions?page=&size=`
- `POST /students/{id}/stars/adjustments`
- `GET /students/trash`: deleted students with their last class (admin)
- `POST /students/{id}/restore` (body `{ "classId"? }`): restore a deleted student with its stars, back to the old class or to `classId` when the old class was deleted (admin)
- `DELETE /students/{id}/permanent`: permanently delete a student in the trash with its scores, star transactions, redemptions and balance (admin)
- `GET /users`, `POST /users`, `PUT /users/{id}`: user management (admin). `classIds` are the classes a teacher can adjust stars in. A locked user, a new role or a new password logs the user out
- `POST /score-imports/preview` (multipart `file`, Excel): read the score sheet and return what will be imported, nothing is saved. Students not on the system are listed in `newStudents` (start date from the "Ngày bắt đầu học" column, `problem` when it cannot be added)
- `POST /score-imports` (multipart `file`, Excel, optional repeated `createStudentCodes`): import new sessions and award stars. The chosen new students are added first. Already imported sessions (same student and date) are skipped
- `GET /score-imports`: finished imports, newest first
- `GET /score-imports/{id}/revert-preview`: stars every student loses when the import is reverted, nothing is saved
- `POST /score-imports/{id}/revert`: take back the stars with reversal transactions and void the scores, so the file can be imported again. Blocked when a student no longer has enough stars

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

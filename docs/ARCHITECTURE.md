# Architecture

## Modules
The frontend is feature-oriented. The backend is a modular monolith split into auth, classroom, student, score, ledger, dashboard, import and audit packages.

## Write path
1. Authenticate JWT and resolve the local user by subject.
2. Authorize role and class assignment.
3. Validate request and idempotency key.
4. Start a database transaction.
5. Lock `student_star_balances` using `FOR UPDATE`.
6. Reject a negative resulting balance.
7. Insert immutable ledger row and update materialized balance.
8. Commit, then return the transaction and new balance.

## Score calculation
`final = homework * 0.60 + test * 0.40`, calculated with Java `BigDecimal`. Stored score keeps scale 2. Star thresholds compare exact decimals and do not compare rounded UI text.
`final = 10` will equal to 3 stars.
`final = 9` will equal to 2 stars.
`final = 8` will equal to 1 star.

## Deployment
Use managed PostgreSQL with automated backups and point-in-time restore. Run at least two stateless backend replicas behind HTTPS. Use a CDN or static host for frontend assets. Export logs and metrics; alert on failed imports, transaction errors and balance reconciliation failures.

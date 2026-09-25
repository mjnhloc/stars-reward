ALTER TABLE users 
    ALTER COLUMN email SET NOT NULL;

ALTER TABLE users
    ADD COLUMN password_hash VARCHAR(100);

CREATE UNIQUE INDEX uq_users_email_lower
    ON users(LOWER(email));

CREATE INDEX ix_users_active_email
    ON users (LOWER(email))
    WHERE active = TRUE;
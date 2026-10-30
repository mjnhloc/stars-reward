CREATE TABLE
    users (
        id uuid PRIMARY KEY,
        external_subject VARCHAR(200) UNIQUE,
        display_name VARCHAR(150) NOT NULL,
        email VARCHAR(250) NOT NULL UNIQUE,
        password_hash VARCHAR(100) NOT NULL,
        role VARCHAR(20) NOT NULL CHECK (role IN ('ADMIN', 'TEACHER', 'VIEWER')),
        active BOOLEAN NOT NULL DEFAULT true,
        updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
        created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
    );

CREATE UNIQUE INDEX uq_users_email_lower
    ON users(LOWER(email));

CREATE INDEX ix_users_active_email
    ON users (LOWER(email))
    WHERE active = TRUE;

CREATE TABLE
    classes (
        id uuid PRIMARY KEY,
        code VARCHAR(50) NOT NULL UNIQUE,
        name VARCHAR(150) NOT NULL,
        school_year VARCHAR(20) NOT NULL,
        archived_at TIMESTAMPTZ,
        created_at TIMESTAMPTZ NOT NULL DEFAULT now ()
    );

CREATE TABLE
    students (
        id uuid PRIMARY KEY,
        student_code VARCHAR(50) NOT NULL UNIQUE,
        full_name VARCHAR(200) NOT NULL,
        archived_at TIMESTAMPTZ,
        created_at TIMESTAMPTZ NOT NULL DEFAULT now ()
    );

CREATE TABLE
    class_enrollments (
        id uuid PRIMARY KEY,
        class_id uuid NOT NULL REFERENCES classes,
        student_id uuid NOT NULL REFERENCES students,
        started_at DATE NOT NULL,
        ended_at DATE,
        UNIQUE (class_id, student_id, started_at)
    );

create UNIQUE index uq_active_enrollment on class_enrollments (student_id)
where
    ended_at is null;

CREATE TABLE
    class_user_assignments (
        user_id uuid NOT NULL REFERENCES users,
        class_id uuid NOT NULL REFERENCES classes,
        can_adjust BOOLEAN NOT NULL DEFAULT false,
        PRIMARY KEY (user_id, class_id)
    );

CREATE TABLE
    import_batches (
        id uuid PRIMARY KEY,
        source_sheet_id VARCHAR(250) NOT NULL,
        status VARCHAR(20) NOT NULL,
        started_at TIMESTAMPTZ NOT NULL,
        completed_at TIMESTAMPTZ,
        created_by uuid REFERENCES users,
        row_count INT NOT NULL DEFAULT 0,
        error_count INT NOT NULL DEFAULT 0
    );

CREATE TABLE
    student_scores (
        id uuid PRIMARY KEY,
        student_id uuid NOT NULL REFERENCES students,
        class_id uuid NOT NULL REFERENCES classes,
        homework_score NUMERIC(4, 2) NOT NULL CHECK (homework_score BETWEEN 0 AND 10),
        test_score NUMERIC(4, 2) NOT NULL CHECK (test_score BETWEEN 0 AND 10),
        final_score NUMERIC(4, 2) NOT NULL CHECK (final_score BETWEEN 0 AND 10),
        awarded_stars INT NOT NULL CHECK (awarded_stars BETWEEN 0 AND 3),
        assessment_date date NOT NULL,
        import_batch_id uuid REFERENCES import_batches,
        external_row_id VARCHAR(200),
        created_at TIMESTAMPTZ NOT NULL DEFAULT NOW (),
        UNIQUE (import_batch_id, external_row_id)
    );

CREATE TABLE
    student_star_balances (
        student_id uuid PRIMARY KEY REFERENCES students,
        total_stars INT NOT NULL DEFAULT 0 CHECK (total_stars >= 0),
        version BIGINT NOT NULL DEFAULT 0,
        updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW ()
    );

CREATE TABLE
    reward_redemptions (
        id uuid PRIMARY KEY,
        student_id uuid NOT NULL REFERENCES students,
        stars_redeemed INT NOT NULL CHECK (stars_redeemed > 0),
        vnd_per_star bigINT NOT NULL CHECK (vnd_per_star > 0),
        amount_vnd bigINT NOT NULL CHECK (amount_vnd > 0),
        status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'PAID', 'CANCELLED')),
        created_by uuid NOT NULL REFERENCES users,
        created_at TIMESTAMPTZ NOT NULL DEFAULT NOW (),
        paid_at TIMESTAMPTZ
    );

CREATE TABLE
    star_transactions (
        id uuid PRIMARY KEY,
        student_id uuid NOT NULL REFERENCES students,
        class_id uuid NOT NULL REFERENCES classes,
        redemption_id uuid REFERENCES reward_redemptions(id),
        transaction_type VARCHAR(20) NOT NULL CHECK (
            transaction_type in ('EARN', 'DEDUCT', 'REDEEM', 'REVERSAL')
        ),
        source_type VARCHAR(20) NOT NULL CHECK (
            source_type IN (
                'SCORE',
                'MANUAL',
                'BEHAVIOR',
                'REDEMPTION',
                'SYSTEM'
            )
        ),
        star_delta INT NOT NULL CHECK (star_delta <> 0),
        balance_before INT NOT NULL CHECK (balance_before >= 0),
        balance_after INT NOT NULL CHECK (balance_after >= 0),
        reason_detail VARCHAR(500) NOT NULL,
        score_id uuid REFERENCES student_scores,
        performed_by uuid NOT NULL REFERENCES users,
        idempotency_key VARCHAR(150) NOT NULL UNIQUE,
        reversed_transaction_id uuid REFERENCES star_transactions,
        created_at TIMESTAMPTZ NOT NULL DEFAULT NOW (),
        CHECK (balance_after = balance_before + star_delta)
    );

create index ix_transactions_student_time on star_transactions (student_id, created_at desc);

CREATE TABLE
    audit_logs (
        id uuid PRIMARY KEY,
        actor_id uuid REFERENCES users,
        action VARCHAR(80) NOT NULL,
        entity_type VARCHAR(80) NOT NULL,
        entity_id uuid,
        details jsonb NOT NULL DEFAULT CAST('{}' AS JSONB),
        created_at TIMESTAMPTZ NOT NULL DEFAULT NOW ()
    );
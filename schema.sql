-- ============================================================
-- College Placement System — MySQL Schema
-- Run this once against a fresh database.
-- Ebean DDL generation is OFF; this is the source of truth.
-- ============================================================

CREATE DATABASE IF NOT EXISTS college_placement;
USE college_placement;

-- ── States ──────────────────────────────────────────────────
CREATE TABLE states (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(100) NOT NULL UNIQUE,
    code            VARCHAR(10)  NOT NULL UNIQUE,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         BOOLEAN      NOT NULL DEFAULT FALSE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── Cities ──────────────────────────────────────────────────
CREATE TABLE cities (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    state_id        BIGINT       NOT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         BOOLEAN      NOT NULL DEFAULT FALSE,
    FOREIGN KEY (state_id) REFERENCES states(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── Users ────────────────────────────────────────────────────
CREATE TABLE users (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    email           VARCHAR(255) NOT NULL UNIQUE,
    name            VARCHAR(255),
    mobile          VARCHAR(20),
    password        VARCHAR(255),
    user_type       VARCHAR(20)  NOT NULL,
    college_id      BIGINT,
    company_id      BIGINT,
    verified        BOOLEAN      NOT NULL DEFAULT FALSE,
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    current_otp     VARCHAR(10),
    avatar_url      VARCHAR(500),
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         BOOLEAN      NOT NULL DEFAULT FALSE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── Refresh Tokens ───────────────────────────────────────────
CREATE TABLE refresh_tokens (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT       NOT NULL,
    token           VARCHAR(500) NOT NULL UNIQUE,
    expires_at      TIMESTAMP,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         BOOLEAN      NOT NULL DEFAULT FALSE,
    FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── Colleges ─────────────────────────────────────────────────
CREATE TABLE colleges (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(255) NOT NULL UNIQUE,
    code            VARCHAR(50)  NOT NULL UNIQUE,
    university      VARCHAR(255),
    address         TEXT,
    city            VARCHAR(100),
    state           VARCHAR(100),
    city_id         BIGINT,
    state_id        BIGINT,
    pincode         VARCHAR(10),
    website         VARCHAR(500),
    logo_url        VARCHAR(500),
    contact_email   VARCHAR(255) NOT NULL,
    contact_phone   VARCHAR(20),
    departments     JSON,
    verified        BOOLEAN      NOT NULL DEFAULT FALSE,
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    attrs           JSON,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         BOOLEAN      NOT NULL DEFAULT FALSE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- FK: users → colleges
ALTER TABLE users ADD FOREIGN KEY (college_id) REFERENCES colleges(id);

-- FK: colleges → cities, states
ALTER TABLE colleges ADD FOREIGN KEY (city_id) REFERENCES cities(id);
ALTER TABLE colleges ADD FOREIGN KEY (state_id) REFERENCES states(id);

-- ── Students ─────────────────────────────────────────────────
CREATE TABLE students (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id             BIGINT       NOT NULL,
    college_id          BIGINT       NOT NULL,
    enrollment_number   VARCHAR(50)  NOT NULL,
    department          VARCHAR(50),
    passing_year        INT,
    cgpa                DECIMAL(4,2),
    active_backlogs     INT          NOT NULL DEFAULT 0,
    total_backlogs      INT          NOT NULL DEFAULT 0,
    tenth_percentage    VARCHAR(10),
    twelfth_percentage  VARCHAR(10),
    diploma_percentage  VARCHAR(10),
    gender              VARCHAR(20),
    date_of_birth       VARCHAR(20),
    skills              JSON,
    certifications      JSON,
    linkedin_url        VARCHAR(500),
    github_url          VARCHAR(500),
    portfolio_url       VARCHAR(500),
    resume_url          VARCHAR(500),
    ats_score           INT,
    opted_out           BOOLEAN      NOT NULL DEFAULT FALSE,
    placed              BOOLEAN      NOT NULL DEFAULT FALSE,
    current_ctc         DECIMAL(12,2),
    attrs               JSON,
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted             BOOLEAN      NOT NULL DEFAULT FALSE,
    FOREIGN KEY (user_id)    REFERENCES users(id),
    FOREIGN KEY (college_id) REFERENCES colleges(id),
    UNIQUE KEY uk_student_enrollment (enrollment_number, college_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── Companies ────────────────────────────────────────────────
CREATE TABLE companies (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    industry        VARCHAR(100),
    website         VARCHAR(500),
    logo_url        VARCHAR(500),
    description     TEXT,
    headquarters    VARCHAR(255),
    contact_email   VARCHAR(255),
    contact_phone   VARCHAR(20),
    startup         BOOLEAN      NOT NULL DEFAULT FALSE,
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    attrs           JSON,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         BOOLEAN      NOT NULL DEFAULT FALSE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- FK: users → companies (for COMPANY_HR users)
ALTER TABLE users ADD FOREIGN KEY (company_id) REFERENCES companies(id);

-- ── Company ↔ College Junction ───────────────────────────────
CREATE TABLE company_colleges (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_id          BIGINT  NOT NULL,
    college_id          BIGINT  NOT NULL,
    managed_by_user_id  BIGINT,
    company_can_manage  BOOLEAN NOT NULL DEFAULT FALSE,
    active              BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted             BOOLEAN NOT NULL DEFAULT FALSE,
    FOREIGN KEY (company_id)         REFERENCES companies(id),
    FOREIGN KEY (college_id)         REFERENCES colleges(id),
    FOREIGN KEY (managed_by_user_id) REFERENCES users(id),
    UNIQUE KEY uk_company_college (company_id, college_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── Drives ───────────────────────────────────────────────────
CREATE TABLE drives (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_college_id      BIGINT       NOT NULL,
    title                   VARCHAR(255) NOT NULL,
    job_description         TEXT,
    employment_type         VARCHAR(30)  NOT NULL,
    academic_year           INT,
    min_cgpa                DECIMAL(4,2),
    max_active_backlogs     INT          NOT NULL DEFAULT 0,
    eligible_departments    JSON,
    eligible_genders        JSON,
    min_passing_year        INT,
    max_passing_year        INT,
    ctc_offered             DECIMAL(12,2),
    stipend                 DECIMAL(12,2),
    location                VARCHAR(255),
    is_remote               BOOLEAN      NOT NULL DEFAULT FALSE,
    registration_deadline   TIMESTAMP    NULL,
    drive_date              TIMESTAMP    NULL,
    status                  VARCHAR(30)  NOT NULL DEFAULT 'UPCOMING',
    venue                   VARCHAR(255),
    required_skills         JSON,
    nice_to_have_skills     JSON,
    attrs                   JSON,
    created_at              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted                 BOOLEAN      NOT NULL DEFAULT FALSE,
    FOREIGN KEY (company_college_id) REFERENCES company_colleges(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── Drive Rounds ─────────────────────────────────────────────
CREATE TABLE drive_rounds (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    drive_id          BIGINT      NOT NULL,
    round_number      INT         NOT NULL,
    round_type        VARCHAR(30) NOT NULL,
    name              VARCHAR(255),
    description       TEXT,
    scheduled_at      TIMESTAMP   NULL,
    duration_minutes  INT,
    venue             VARCHAR(255),
    completed         BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted           BOOLEAN     NOT NULL DEFAULT FALSE,
    FOREIGN KEY (drive_id) REFERENCES drives(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── Drive Applications ───────────────────────────────────────
CREATE TABLE drive_applications (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id      BIGINT      NOT NULL,
    drive_id        BIGINT      NOT NULL,
    status          VARCHAR(30) NOT NULL DEFAULT 'ELIGIBLE',
    resume_snapshot VARCHAR(500),
    notes           TEXT,
    created_at      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         BOOLEAN     NOT NULL DEFAULT FALSE,
    FOREIGN KEY (student_id) REFERENCES students(id),
    FOREIGN KEY (drive_id)   REFERENCES drives(id),
    UNIQUE KEY uk_student_drive (student_id, drive_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── Round Results ────────────────────────────────────────────
CREATE TABLE round_results (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    round_id          BIGINT        NOT NULL,
    student_id        BIGINT        NOT NULL,
    status            VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    score             DECIMAL(8,2),
    feedback          TEXT,
    interviewer_name  VARCHAR(255),
    created_at        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted           BOOLEAN       NOT NULL DEFAULT FALSE,
    FOREIGN KEY (round_id)   REFERENCES drive_rounds(id),
    FOREIGN KEY (student_id) REFERENCES students(id),
    UNIQUE KEY uk_round_student (round_id, student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── Offers ───────────────────────────────────────────────────
CREATE TABLE offers (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id          BIGINT        NOT NULL,
    drive_id            BIGINT        NOT NULL,
    ctc_offered         DECIMAL(12,2) NOT NULL,
    designation         VARCHAR(255),
    location            VARCHAR(255),
    status              VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    response_deadline   TIMESTAMP     NULL,
    responded_at        TIMESTAMP     NULL,
    offer_letter_url    VARCHAR(500),
    notes               TEXT,
    created_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted             BOOLEAN       NOT NULL DEFAULT FALSE,
    FOREIGN KEY (student_id) REFERENCES students(id),
    FOREIGN KEY (drive_id)   REFERENCES drives(id),
    UNIQUE KEY uk_offer_student_drive (student_id, drive_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── Placement Policies ───────────────────────────────────────
CREATE TABLE placement_policies (
    id                       BIGINT AUTO_INCREMENT PRIMARY KEY,
    college_id               BIGINT        NOT NULL,
    academic_year            INT           NOT NULL,
    dream_ctc_threshold      DECIMAL(12,2),
    max_simultaneous_offers  INT           NOT NULL DEFAULT 1,
    block_after_first_accept BOOLEAN       NOT NULL DEFAULT FALSE,
    auto_filter_enabled      BOOLEAN       NOT NULL DEFAULT TRUE,
    offer_expiry_days        INT           NOT NULL DEFAULT 7,
    description              TEXT,
    created_at               TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at               TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted                  BOOLEAN       NOT NULL DEFAULT FALSE,
    FOREIGN KEY (college_id) REFERENCES colleges(id),
    UNIQUE KEY uk_policy_college_year (college_id, academic_year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── Notifications ────────────────────────────────────────────
CREATE TABLE notifications (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    college_id      BIGINT       NOT NULL,
    drive_id        BIGINT,
    channel         VARCHAR(20)  NOT NULL,
    type            VARCHAR(50)  NOT NULL,
    subject         VARCHAR(500),
    body            TEXT,
    recipient_count INT          NOT NULL DEFAULT 0,
    delivered_count INT          NOT NULL DEFAULT 0,
    failed_count    INT          NOT NULL DEFAULT 0,
    sent_at         TIMESTAMP    NULL,
    metadata        JSON,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         BOOLEAN      NOT NULL DEFAULT FALSE,
    FOREIGN KEY (college_id) REFERENCES colleges(id),
    FOREIGN KEY (drive_id)   REFERENCES drives(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── Documents ────────────────────────────────────────────────
CREATE TABLE documents (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    college_id          BIGINT       NOT NULL,
    company_id          BIGINT,
    name                VARCHAR(255) NOT NULL,
    type                VARCHAR(50)  NOT NULL,
    file_url            VARCHAR(500) NOT NULL,
    file_type           VARCHAR(20),
    file_size_bytes     BIGINT,
    academic_year       INT,
    uploaded_by_user_id BIGINT,
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted             BOOLEAN      NOT NULL DEFAULT FALSE,
    FOREIGN KEY (college_id)          REFERENCES colleges(id),
    FOREIGN KEY (company_id)          REFERENCES companies(id),
    FOREIGN KEY (uploaded_by_user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── PYQ (Previous Year Questions) ────────────────────────────
CREATE TABLE pyqs (
    id                          BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_id                  BIGINT      NOT NULL,
    college_id                  BIGINT,
    role                        VARCHAR(100),
    round_type                  VARCHAR(30) NOT NULL,
    year                        INT,
    content                     TEXT        NOT NULL,
    difficulty                  VARCHAR(20),
    tags                        JSON,
    upvotes                     INT         NOT NULL DEFAULT 0,
    anonymous                   BOOLEAN     NOT NULL DEFAULT TRUE,
    contributed_by_student_id   BIGINT,
    created_at                  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted                     BOOLEAN     NOT NULL DEFAULT FALSE,
    FOREIGN KEY (company_id)                REFERENCES companies(id),
    FOREIGN KEY (college_id)                REFERENCES colleges(id),
    FOREIGN KEY (contributed_by_student_id) REFERENCES students(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── Subscriptions ────────────────────────────────────────────
CREATE TABLE subscriptions (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id          BIGINT,
    college_id          BIGINT,
    tier                VARCHAR(20)   NOT NULL DEFAULT 'FREE',
    start_date          TIMESTAMP     NULL,
    end_date            TIMESTAMP     NULL,
    total_credits       INT           NOT NULL DEFAULT 50,
    used_credits        INT           NOT NULL DEFAULT 0,
    credits_reset_at    TIMESTAMP     NULL,
    payment_reference   VARCHAR(255),
    active              BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted             BOOLEAN       NOT NULL DEFAULT FALSE,
    FOREIGN KEY (student_id) REFERENCES students(id),
    FOREIGN KEY (college_id) REFERENCES colleges(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── Credit Transactions ─────────────────────────────────────
CREATE TABLE credit_transactions (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    subscription_id     BIGINT       NOT NULL,
    student_id          BIGINT,
    college_id          BIGINT,
    type                VARCHAR(50)  NOT NULL,
    amount              INT          NOT NULL,
    balance_after       INT          NOT NULL,
    description         VARCHAR(500),
    payment_reference   VARCHAR(255),
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted             BOOLEAN      NOT NULL DEFAULT FALSE,
    FOREIGN KEY (subscription_id) REFERENCES subscriptions(id),
    FOREIGN KEY (student_id)      REFERENCES students(id),
    FOREIGN KEY (college_id)      REFERENCES colleges(id),
    INDEX idx_ct_subscription (subscription_id),
    INDEX idx_ct_student (student_id),
    INDEX idx_ct_college (college_id),
    INDEX idx_ct_type (type),
    INDEX idx_ct_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── Invite Tokens ───────────────────────────────────────────
CREATE TABLE invite_tokens (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    college_id      BIGINT       NOT NULL,
    token           VARCHAR(255) NOT NULL UNIQUE,
    email           VARCHAR(255),
    department      VARCHAR(50),
    passing_year    INT,
    used            BOOLEAN      NOT NULL DEFAULT FALSE,
    expires_at      TIMESTAMP    NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted         BOOLEAN      NOT NULL DEFAULT FALSE,
    FOREIGN KEY (college_id) REFERENCES colleges(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── Indexes for common queries ───────────────────────────────
CREATE INDEX idx_users_email           ON users(email);
CREATE INDEX idx_users_college         ON users(college_id);
CREATE INDEX idx_students_college      ON students(college_id);
CREATE INDEX idx_students_placed       ON students(college_id, placed);
CREATE INDEX idx_drives_cc             ON drives(company_college_id);
CREATE INDEX idx_drives_status         ON drives(status);
CREATE INDEX idx_drive_apps_drive      ON drive_applications(drive_id);
CREATE INDEX idx_drive_apps_student    ON drive_applications(student_id);
CREATE INDEX idx_offers_student        ON offers(student_id);
CREATE INDEX idx_offers_drive          ON offers(drive_id);
CREATE INDEX idx_round_results_round   ON round_results(round_id);
CREATE INDEX idx_notifications_college ON notifications(college_id);
CREATE INDEX idx_pyqs_company          ON pyqs(company_id);

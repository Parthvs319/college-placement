-- ============================================================
-- Migration V2: College Contracts & Invoices
-- Run after schema.sql (V1)
-- ============================================================

-- ── College Contracts ────────────────────────────────────────
-- Stores metadata about the signed contract/MOU for each college.
-- The actual file is in S3; the document record is in college_documents.
CREATE TABLE college_contracts (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    college_id          BIGINT         NOT NULL,
    document_id         BIGINT         NOT NULL,    -- FK → college_documents (the uploaded file)
    contract_amount     DECIMAL(15, 2) NOT NULL,    -- Annual contract value (INR)
    valid_from          VARCHAR(20),                -- ISO date e.g. "2025-01-01"
    valid_to            VARCHAR(20),                -- ISO date e.g. "2025-12-31"
    tpo_user_id         BIGINT,                     -- TPO user account created during onboarding
    status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE | EXPIRED | TERMINATED
    notes               TEXT,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted             BOOLEAN   NOT NULL DEFAULT FALSE,
    FOREIGN KEY (college_id)   REFERENCES colleges(id),
    FOREIGN KEY (document_id)  REFERENCES college_documents(id),
    FOREIGN KEY (tpo_user_id)  REFERENCES users(id),
    INDEX idx_contracts_college (college_id),
    INDEX idx_contracts_status  (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── College Invoices ─────────────────────────────────────────
-- One invoice per billing cycle per college.
-- PDF is generated on demand and stored in S3.
CREATE TABLE college_invoices (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    college_id           BIGINT         NOT NULL,
    contract_id          BIGINT,                     -- FK → college_contracts (nullable for legacy)
    invoice_number       VARCHAR(100)   NOT NULL UNIQUE, -- e.g. INV-IITD-2025-001
    contract_amount      DECIMAL(15, 2),             -- snapshot of contract amount at generation time
    billing_period_start VARCHAR(20),                -- ISO date e.g. "2025-01-01"
    billing_period_end   VARCHAR(20),                -- ISO date e.g. "2025-03-31"
    amount               DECIMAL(15, 2) NOT NULL,    -- invoice total
    description          TEXT,
    status               VARCHAR(20) NOT NULL DEFAULT 'DRAFT',   -- DRAFT | SENT | PAID | OVERDUE | CANCELLED
    s3_key               VARCHAR(500),               -- S3 object key for the PDF
    file_url             VARCHAR(1000),              -- Public or pre-signed URL
    generated_by_user_id BIGINT,                     -- super admin who triggered generation
    created_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted              BOOLEAN   NOT NULL DEFAULT FALSE,
    FOREIGN KEY (college_id)            REFERENCES colleges(id),
    FOREIGN KEY (contract_id)           REFERENCES college_contracts(id),
    FOREIGN KEY (generated_by_user_id)  REFERENCES users(id),
    INDEX idx_invoices_college  (college_id),
    INDEX idx_invoices_contract (contract_id),
    INDEX idx_invoices_status   (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

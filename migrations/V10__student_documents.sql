-- V10: Student document uploads and verification
-- Stores identity and academic documents uploaded by students.
-- Document types: COLLEGE_ID, MARKSHEET, RESUME, AADHAR_CARD, PAN_CARD

CREATE TABLE IF NOT EXISTS student_documents (
    id                BIGSERIAL PRIMARY KEY,
    student_id        BIGINT NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    document_type     VARCHAR(50)  NOT NULL,          -- COLLEGE_ID | MARKSHEET | RESUME | AADHAR_CARD | PAN_CARD
    label             VARCHAR(255) NOT NULL,           -- e.g. "Semester 5 Marksheet"
    file_name         VARCHAR(255),
    file_url          TEXT         NOT NULL,
    content_type      VARCHAR(100),
    file_size_bytes   BIGINT,
    semester          INTEGER,                         -- 1-8, only for MARKSHEET
    verified          BOOLEAN      NOT NULL DEFAULT FALSE,
    verification_note TEXT,
    deleted           BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at        TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_student_documents_student_id ON student_documents(student_id);
CREATE INDEX IF NOT EXISTS idx_student_documents_type       ON student_documents(student_id, document_type);

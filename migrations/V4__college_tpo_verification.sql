-- ============================================================
-- Migration V4: TPO verification flags + TPO name + GSTIN on colleges
-- ============================================================

ALTER TABLE colleges
    ADD COLUMN gstin             VARCHAR(15)   NULL          AFTER website,
    ADD COLUMN tpo_name          VARCHAR(255)  NULL          AFTER contact_phone,
    ADD COLUMN is_email_verified BOOLEAN NOT NULL DEFAULT FALSE AFTER tpo_name,
    ADD COLUMN is_phone_verified BOOLEAN NOT NULL DEFAULT FALSE AFTER is_email_verified;

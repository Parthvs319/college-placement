package models.sql;

import helpers.blueprint.models.BaseModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;

/**
 * Documents uploaded by or for a company.
 * Examples: GST Certificate, Certificate of Incorporation,
 * Applyra Contract/MOU, NDA, Company Profile, etc.
 */
@EqualsAndHashCode(callSuper = true)
@Entity
@Data
@Table(name = "company_documents")
public class CompanyDocument extends BaseModel {

    @ManyToOne
    @JoinColumn(name = "company_id", nullable = false)
    public Company company;

    /**
     * Document category:
     * APPLYRA_CONTRACT, GST_CERTIFICATE, INCORPORATION_CERTIFICATE,
     * NDA, COMPANY_PROFILE, PAN_CARD, TAN_CERTIFICATE,
     * MSME_REGISTRATION, OTHER
     */
    @Column(nullable = false)
    public String documentType;

    /** Human-readable label, e.g. "GST Certificate" */
    @Column(nullable = false)
    public String label;

    /** Original uploaded file name */
    public String fileName;

    /** S3 or public URL to the file */
    @Column(nullable = false)
    public String fileUrl;

    /** MIME type (application/pdf, image/jpeg, etc.) */
    public String contentType;

    /** File size in bytes */
    public Long fileSizeBytes;

    /** Expiry date for time-bound documents (ISO string) */
    public String expiryDate;

    /** Whether this document has been verified by super admin */
    public boolean verified = false;

    /** Optional verification note from admin */
    public String verificationNote;
}

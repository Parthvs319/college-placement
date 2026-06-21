package models.sql;

import helpers.blueprint.models.BaseModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;

/**
 * Documents uploaded by or for a college.
 * Examples: AICTE Approval, NAAC Certificate, University Affiliation,
 * Applyra Contract/MOU, Placement Policy, etc.
 */
@EqualsAndHashCode(callSuper = true)
@Entity
@Data
@Table(name = "college_documents")
public class CollegeDocument extends BaseModel {

    @ManyToOne
    @JoinColumn(name = "college_id", nullable = false)
    public College college;

    /**
     * Document category:
     * APPLYRA_CONTRACT, MOU, AICTE_APPROVAL, NAAC_CERTIFICATE,
     * UNIVERSITY_AFFILIATION, PLACEMENT_POLICY, ACCREDITATION,
     * TRUST_REGISTRATION, OTHER
     */
    @Column(nullable = false)
    public String documentType;

    /** Human-readable label, e.g. "AICTE Approval Letter 2025" */
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

    /** Academic year this document pertains to, e.g. 2025 */
    public Integer academicYear;

    /** Expiry date for time-bound documents (ISO string) */
    public String expiryDate;

    /** Whether this document has been verified by super admin */
    public boolean verified = false;

    /** Optional verification note from admin */
    public String verificationNote;
}

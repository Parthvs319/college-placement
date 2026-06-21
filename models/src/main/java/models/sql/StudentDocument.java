package models.sql;

import helpers.blueprint.models.BaseModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;

/**
 * Identity and academic documents uploaded by a student.
 * Examples: Aadhaar, PAN, Student ID, Marksheets (per semester), Resume, etc.
 */
@EqualsAndHashCode(callSuper = true)
@Entity
@Data
@Table(name = "student_documents")
public class StudentDocument extends BaseModel {

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    public Student student;

    /** Document category: AADHAAR, PAN, STUDENT_ID, RESUME, MARKSHEET, OTHER */
    @Column(nullable = false)
    public String documentType;

    /** Human-readable label, e.g. "Semester 3 Marksheet", "Aadhaar Card" */
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

    /** Optional: semester number for marksheets (1-8) */
    public Integer semester;

    /** Whether this document has been verified by TPO/admin */
    public boolean verified = false;

    /** Optional verification note from admin */
    public String verificationNote;
}

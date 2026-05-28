package models.sql;

import helpers.blueprint.models.BaseModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;

/**
 * Represents a student's uploaded resume.
 * Students can upload multiple resumes and mark one as primary.
 * The primary resume is used for placement applications and AI features.
 */
@EqualsAndHashCode(callSuper = true)
@Entity
@Data
@Table(name = "resumes")
public class Resume extends BaseModel {

    @ManyToOne
    @JoinColumn(name = "student_id", nullable = false)
    public Student student;

    @Column(nullable = false)
    public String fileName;              // original uploaded file name

    @Column(nullable = false)
    public String s3Key;                 // S3 object key (e.g. "resumes/uuid.pdf")

    @Column(nullable = false)
    public String url;                   // public or pre-signed URL

    public String contentType;           // MIME type (application/pdf, etc.)

    public long fileSize;                // size in bytes

    @Column(name = "`primary`", nullable = false)
    public boolean primary = false;      // only one resume should be primary per student

    public String label;                 // optional user label (e.g. "SDE Resume", "Data Science Resume")

    public Integer atsScore;             // ATS score if calculated
}

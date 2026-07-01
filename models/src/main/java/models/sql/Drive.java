package models.sql;

import helpers.blueprint.models.AttrsModel;
import io.ebean.annotation.DbJsonB;
import lombok.Data;
import lombok.EqualsAndHashCode;
import models.enums.DriveStatus;
import models.enums.EmploymentType;

import javax.persistence.*;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

/**
 * A placement drive = a company visiting a college for hiring.
 * Contains JD, eligibility criteria, schedule, and round structure.
 */
@EqualsAndHashCode(callSuper = true)
@Entity
@Data
@Table(name = "drives")
public class Drive extends AttrsModel {

    public String driveCode;

    @ManyToOne
    @JoinColumn(name = "company_college_id", nullable = false)
    public CompanyCollege companyCollege;

    @Column(nullable = false)
    public String title;                    // "SDE Intern 2026", "Analyst Full-Time"

    public String jobDescription;

    /** S3 URL to the uploaded JD document (PDF/DOCX) */
    public String jdFileUrl;

    @Column(nullable = false)
    public EmploymentType employmentType;

    public int academicYear;                // e.g. 2026

    // ── Eligibility Criteria ──────────────────────────────
    public BigDecimal minCgpa;

    public int maxActiveBacklogs = 0;

    @DbJsonB
    public List<String> eligibleDepartments;  // ["CSE", "IT", "ECE"] — empty = all

    @DbJsonB
    public List<String> eligibleGenders;      // null = all

    public Integer minPassingYear;

    public Integer maxPassingYear;

    // ── Compensation ──────────────────────────────────────
    public BigDecimal ctcOffered;             // annual CTC in INR

    public BigDecimal stipend;                // for internships

    public String location;                   // job location

    public boolean isRemote = false;

    // ── Schedule ──────────────────────────────────────────
    public Timestamp registrationDeadline;

    public Timestamp driveDate;               // on-campus date

    @Column(nullable = false)
    public DriveStatus status = DriveStatus.UPCOMING;

    public String venue;                      // room / building

    // ── Relationships ─────────────────────────────────────
    @OneToMany(mappedBy = "drive")
    public List<DriveRound> rounds;

    @OneToMany(mappedBy = "drive")
    public List<DriveApplication> applications;

    @OneToMany(mappedBy = "drive")
    public List<Offer> offers;

    @DbJsonB
    public List<String> requiredSkills;

    @DbJsonB
    public List<String> niceToHaveSkills;
}

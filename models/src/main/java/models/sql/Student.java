package models.sql;

import helpers.blueprint.models.AttrsModel;
import io.ebean.annotation.DbJsonB;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Entity
@Data
@Table(name = "students")
public class Student extends AttrsModel {

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    public User user;

    @ManyToOne
    @JoinColumn(name = "college_id", nullable = false)
    public College college;

    @Column(nullable = false)
    public String enrollmentNumber;         // university roll number

    public String department;               // CSE, IT, ECE, etc.

    public int passingYear;                 // graduation year

    public BigDecimal cgpa;

    public int activeBacklogs = 0;

    public int totalBacklogs = 0;

    public String tenthPercentage;

    public String twelfthPercentage;

    public String diplomaPercentage;        // null if not applicable

    public String gender;

    public String aadharNumber;          // 12-digit Aadhaar (stored masked in display)

    public String panNumber;             // 10-char PAN

    public String studentCollegeId;      // internal college student ID (different from enrollmentNumber)

    /** Reservation category: GENERAL, OBC, SC, ST, EWS */
    public String category;

    /** True once student submits their profile for the first time */
    public boolean profileComplete = false;

    public java.sql.Timestamp profileSubmittedAt;

    public String dateOfBirth;

    @DbJsonB
    public List<String> skills;

    @DbJsonB
    public List<String> certifications;

    public String linkedinUrl;

    public String githubUrl;

    public String portfolioUrl;

    /**
     * @deprecated Use {@link Resume} entity instead. Kept for backward compatibility.
     */
    @Deprecated
    public String resumeUrl;                // legacy single resume URL

    public Integer atsScore;                // from primary resume ATS score

    @OneToMany(mappedBy = "student")
    public List<Resume> resumes;

    public boolean optedOut = false;         // student opted out of placement

    // current placement status
    public boolean placed = false;

    public Timestamp placedAt;              // when the student got placed (offer accepted)

    public BigDecimal currentCtc;           // CTC of accepted offer (if any)

    public boolean internship = false;     // student completed/accepted an internship
    public boolean ppo = false;            // student received a Pre-Placement Offer

    @OneToMany(mappedBy = "student")
    public List<StudentDocument> documents;

    @OneToMany(mappedBy = "student")
    public List<DriveApplication> applications;

    @OneToMany(mappedBy = "student")
    public List<Offer> offers;
}

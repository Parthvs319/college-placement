package models.sql;

import helpers.blueprint.models.AttrsModel;
import io.ebean.annotation.DbJsonB;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.math.BigDecimal;
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

    public String dateOfBirth;

    @DbJsonB
    public List<String> skills;

    @DbJsonB
    public List<String> certifications;

    public String linkedinUrl;

    public String githubUrl;

    public String portfolioUrl;

    public String resumeUrl;                // S3 or Applyra resume link

    public Integer atsScore;                // from Applyra integration

    public boolean optedOut = false;         // student opted out of placement

    // current placement status
    public boolean placed = false;

    public BigDecimal currentCtc;           // CTC of accepted offer (if any)

    @OneToMany(mappedBy = "student")
    public List<DriveApplication> applications;

    @OneToMany(mappedBy = "student")
    public List<Offer> offers;
}

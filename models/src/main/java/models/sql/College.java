package models.sql;

import helpers.blueprint.models.AttrsModel;
import io.ebean.annotation.DbJsonB;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Entity
@Data
@Table(name = "colleges")
public class College extends AttrsModel {

    @Column(nullable = false)
    public String name;

    @Column(nullable = false)
    public String code;                     // e.g. "SGSITS", "IET-DAVV"

    public String university;               // affiliated university

    public String address;

    public Long cityId;

    public Long stateId;

    public String pincode;

    public String website;

    /** GST Identification Number — used to auto-populate college details during onboarding */
    public String gstin;

    /** AISHE Code — set when the college is matched against the national AISHE registry during onboarding */
    public String aisheCode;

    public String logoUrl;

    @Column(nullable = false)
    public String contactEmail;

    public String contactPhone;

    /** Full name of the TPO (Placement Officer) — collected during onboarding */
    public String tpoName;

    /** Whether the contactEmail was OTP-verified during onboarding */
    public boolean isEmailVerified = false;

    /** Whether the contactPhone was OTP-verified during onboarding */
    public boolean isPhoneVerified = false;

    @DbJsonB
    public List<String> departments;        // ["CSE", "IT", "ECE", "ME", ...]

    public boolean verified = false;

    public boolean active = true;

    @OneToMany(mappedBy = "college")
    public List<Student> students;

    @OneToMany(mappedBy = "college")
    public List<CompanyCollege> companyColleges;

    @OneToMany(mappedBy = "college")
    public List<CollegeDocument> documents;
}

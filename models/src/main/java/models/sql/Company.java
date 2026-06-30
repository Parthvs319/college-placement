package models.sql;

import helpers.blueprint.models.AttrsModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Entity
@Data
@Table(name = "companies")
public class Company extends AttrsModel {

    public String code;

    @Column(nullable = false)
    public String name;

    public String industry;                 // IT, Finance, Manufacturing, etc.

    // ── Company Identity ──────────────────────────────────────────────────────

    public String companyType;              // PUBLIC_LTD, PRIVATE_LTD, LLP, STARTUP, MNC, GOVERNMENT_PSU

    public String cin;                      // Corporate Identification Number (21 chars)

    public String gstin;                    // GST Identification Number (15 chars)

    public Integer yearOfEstablishment;

    public String employeeCount;            // e.g. "1-50", "51-200", "201-1000", "1000+"

    public String linkedinUrl;

    public String website;

    public String logoUrl;

    public String description;

    public String headquarters;

    public String contactEmail;

    public String contactPhone;

    public boolean startup = false;

    public boolean active = true;

    public boolean onboardedByCollege = false;

    public boolean selfOnboard = false;

    // ── HR Contact ────────────────────────────────────────────────────────────

    public String hrDesignation;            // e.g. "Talent Acquisition Manager"

    public String hrLinkedin;

    // ── Relations ─────────────────────────────────────────────────────────────

    @OneToMany(mappedBy = "company")
    public List<CompanyCollege> companyColleges;

    @OneToMany(mappedBy = "company")
    public List<CompanyDocument> documents;
}

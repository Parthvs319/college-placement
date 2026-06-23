package models.sql;

import helpers.blueprint.models.BaseModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.math.BigDecimal;

/**
 * Metadata for a signed Applyra contract/MOU with a college.
 * The actual PDF file is stored in college_documents (documentType = APPLYRA_CONTRACT).
 */
@EqualsAndHashCode(callSuper = true)
@Entity
@Data
@Table(name = "college_contracts")
public class CollegeContract extends BaseModel {

    @ManyToOne
    @JoinColumn(name = "college_id", nullable = false)
    public College college;

    /**
     * Human-readable contract number, auto-generated as: Contract-{CollegeCode}-{NNN}
     * e.g. Contract-SDCES-001
     */
    @Column(unique = true)
    public String contractNumber;

    /** The uploaded contract document (nullable for FREE_TRIAL contracts with no PDF yet) */
    @ManyToOne
    @JoinColumn(name = "document_id")
    public CollegeDocument document;

    /** Annual contract value in INR */
    @Column(nullable = false, precision = 15, scale = 2)
    public BigDecimal contractAmount;

    /** PAID | FREE_TRIAL */
    @Column(nullable = false)
    public String contractType = "PAID";

    /** ISO date string "YYYY-MM-DD" */
    public String validFrom;

    /** ISO date string "YYYY-MM-DD" */
    public String validTo;

    /** TPO user account created during onboarding (may be null if TPO registered themselves) */
    @ManyToOne
    @JoinColumn(name = "tpo_user_id")
    public User tpoUser;

    /** ACTIVE | EXPIRED | TERMINATED */
    @Column(nullable = false)
    public String status = "ACTIVE";

    /** MONTHLY | YEARLY — billing cycle for invoice generation */
    @Column(nullable = false)
    public String payType = "MONTHLY";

    public String notes;
}

package models.sql;

import helpers.blueprint.models.BaseModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.math.BigDecimal;

/**
 * A generated invoice for a college billing cycle.
 * PDF is generated on demand and stored in S3.
 */
@EqualsAndHashCode(callSuper = true)
@Entity
@Data
@Table(name = "college_invoices")
public class CollegeInvoice extends BaseModel {

    @ManyToOne
    @JoinColumn(name = "college_id", nullable = false)
    public College college;

    @ManyToOne
    @JoinColumn(name = "contract_id")
    public CollegeContract contract;

    /** e.g. INV-IITD-2025-001 */
    @Column(nullable = false, unique = true)
    public String invoiceNumber;

    /** Snapshot of contract amount at time of generation */
    @Column(precision = 15, scale = 2)
    public BigDecimal contractAmount;

    /** Billing period start — ISO date string "YYYY-MM-DD" */
    public String billingPeriodStart;

    /** Billing period end — ISO date string "YYYY-MM-DD" */
    public String billingPeriodEnd;

    /** Invoice total amount */
    @Column(nullable = false, precision = 15, scale = 2)
    public BigDecimal amount;

    public String description;

    /** DRAFT | SENT | PAID | OVERDUE | CANCELLED */
    @Column(nullable = false)
    public String status = "DRAFT";

    // ── Payment details (populated when status = PAID) ────────
    /** UPI | NEFT | RTGS | IMPS | CASH | CHEQUE */
    @Column(length = 20)
    public String paymentMode;

    /** UTR / transaction ID / cheque number / UPI ref */
    @Column(length = 255)
    public String paymentReference;

    /** ISO datetime when payment was received e.g. "2025-06-22T14:30:00" */
    @Column(length = 30)
    public String paidAt;

    /** Name of the payer / bank */
    @Column(length = 100)
    public String paidByName;

    @Column(columnDefinition = "TEXT")
    public String paymentNotes;

    /** S3 object key for the generated PDF */
    public String s3Key;

    /** Public or pre-signed URL for download */
    public String fileUrl;

    /** Super admin who triggered generation */
    @ManyToOne
    @JoinColumn(name = "generated_by_user_id")
    public User generatedBy;
}

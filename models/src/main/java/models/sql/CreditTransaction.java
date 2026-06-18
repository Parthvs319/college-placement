package models.sql;

import helpers.blueprint.models.BaseModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import models.enums.CreditTransactionType;

import javax.persistence.*;

/**
 * Logs every credit usage and top-up.
 * Provides full audit trail of AI credit consumption and purchases.
 */
@EqualsAndHashCode(callSuper = true)
@Entity
@Data
@Table(name = "credit_transactions")
public class CreditTransaction extends BaseModel {

    @ManyToOne
    @JoinColumn(name = "subscription_id", nullable = false)
    public Subscription subscription;

    @ManyToOne
    @JoinColumn(name = "student_id")
    public Student student;

    @ManyToOne
    @JoinColumn(name = "college_id")
    public College college;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    public CreditTransactionType type;

    /**
     * Positive for top-ups, negative for usage.
     * e.g. +100 for a payment top-up, -1 for a resume score.
     */
    @Column(nullable = false)
    public int amount;

    /**
     * Balance on the subscription AFTER this transaction.
     * Stored as (totalCredits - usedCredits) at time of transaction.
     */
    @Column(nullable = false)
    public int balanceAfter;

    /**
     * Human-readable description.
     * e.g. "AI Resume Score for Google SDE drive", "Payment top-up - 200 credits"
     */
    public String description;

    /**
     * Payment reference for top-up transactions (Razorpay ID, etc.)
     */
    public String paymentReference;
}

package models.sql;

import helpers.blueprint.models.BaseModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import models.enums.SubscriptionTier;

import javax.persistence.*;
import java.sql.Timestamp;

/**
 * Premium subscription — can be student-level or college-level.
 *
 * Student-level: individual student pays for premium AI features.
 * College-level: college subscribes → all students at that college get premium access.
 *
 * Tracks credits for AI feature usage (rate limiting).
 */
@EqualsAndHashCode(callSuper = true)
@Entity
@Data
@Table(name = "subscriptions")
public class Subscription extends BaseModel {

    /** If student-level subscription, this is set */
    @ManyToOne
    @JoinColumn(name = "student_id")
    public Student student;

    /** If college-level subscription, this is set */
    @ManyToOne
    @JoinColumn(name = "college_id")
    public College college;

    @Column(nullable = false)
    public SubscriptionTier tier = SubscriptionTier.FREE;

    /** When the subscription started */
    @Column(nullable = false)
    public Timestamp startDate;

    /** When the subscription expires (null = never expires) */
    public Timestamp endDate;

    /** Monthly AI credits (ATS scores, JD matches, resume generation) */
    public int totalCredits = 50;

    /** Credits used this month */
    public int usedCredits = 0;

    /** Last credit reset date */
    public Timestamp creditsResetAt;

    /** Payment reference (Razorpay/Stripe payment ID) */
    public String paymentReference;

    /** Is subscription currently active */
    public boolean active = true;

    /**
     * Check if subscription has remaining credits.
     */
    public boolean hasCredits() {
        return usedCredits < totalCredits;
    }

    /**
     * Use one credit. Returns false if no credits left.
     */
    public boolean useCredit() {
        if (usedCredits >= totalCredits) return false;
        usedCredits++;
        this.update();
        return true;
    }
}

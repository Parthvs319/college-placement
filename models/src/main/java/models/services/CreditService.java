package models.services;

import helpers.customErrors.RoutingError;
import models.enums.CreditTransactionType;
import models.repos.CreditTransactionRepository;
import models.repos.SubscriptionRepository;
import models.sql.CreditTransaction;
import models.sql.Student;
import models.sql.Subscription;

import java.util.List;

/**
 * Manages AI credit operations for students and colleges.
 *
 * Credit flow:
 * 1. Student uses an AI feature (resume score, mock interview, etc.)
 * 2. CreditService checks student's own subscription first, then college pool
 * 3. If credits available, deducts 1 and logs a CreditTransaction
 * 4. If exhausted, throws error asking user to top up
 *
 * Top-up flow:
 * 1. Payment is confirmed (Razorpay callback, admin manual, etc.)
 * 2. CreditService adds credits to the subscription and logs transaction
 */
public enum CreditService {
    INSTANCE;

    /**
     * Consume one credit for an AI feature.
     * Tries student subscription first, then college pool.
     * Logs a CreditTransaction with the deduction.
     *
     * @param student The student using the feature
     * @param type    The type of AI feature (AI_RESUME_SCORE, AI_MOCK_INTERVIEW, etc.)
     * @param description Human-readable description (e.g. "Resume score for Google SDE drive")
     * @return The subscription that was charged
     * @throws RoutingError if no active subscription or credits exhausted
     */
    public Subscription consumeCredit(Student student, CreditTransactionType type, String description) {
        // Try student-level subscription first
        Subscription sub = SubscriptionRepository.INSTANCE.activeByStudent(student.getId());
        if (sub == null) {
            // Try college-level pool
            sub = SubscriptionRepository.INSTANCE.activeByCollege(student.college.getId());
        }
        if (sub == null) {
            throw new RoutingError("Premium subscription required. Upgrade to access AI features.");
        }

        if (!sub.hasCredits()) {
            int remaining = sub.totalCredits - sub.usedCredits;
            throw new RoutingError("AI credits exhausted (" + remaining + " remaining). Please top up to continue.");
        }

        // Deduct credit
        sub.usedCredits++;
        sub.update();

        // Log transaction
        int balanceAfter = sub.totalCredits - sub.usedCredits;
        logTransaction(sub, student, type, -1, balanceAfter, description, null);

        return sub;
    }

    /**
     * Top up credits on a subscription after payment.
     *
     * @param subscriptionId The subscription to top up
     * @param credits        Number of credits to add
     * @param paymentRef     Payment reference (Razorpay ID, etc.)
     * @param description    Human-readable description
     * @return The updated subscription
     */
    public Subscription topUpCredits(Long subscriptionId, int credits, String paymentRef, String description) {
        Subscription sub = SubscriptionRepository.INSTANCE.byId(subscriptionId);
        if (sub == null) {
            throw new RoutingError("Subscription not found");
        }

        // Add credits by increasing totalCredits
        sub.totalCredits += credits;
        sub.paymentReference = paymentRef;
        sub.update();

        int balanceAfter = sub.totalCredits - sub.usedCredits;
        logTransaction(sub, sub.student, CreditTransactionType.PAYMENT_TOPUP, credits, balanceAfter, description, paymentRef);

        return sub;
    }

    /**
     * Admin manually adds credits (bonus, refund, etc.)
     */
    public Subscription adminTopUp(Long subscriptionId, int credits, CreditTransactionType type, String description) {
        Subscription sub = SubscriptionRepository.INSTANCE.byId(subscriptionId);
        if (sub == null) {
            throw new RoutingError("Subscription not found");
        }

        sub.totalCredits += credits;
        sub.update();

        int balanceAfter = sub.totalCredits - sub.usedCredits;
        logTransaction(sub, sub.student, type, credits, balanceAfter, description, null);

        return sub;
    }

    /**
     * Get remaining credits for a student (checks both student and college pool).
     */
    public int getBalance(Student student) {
        Subscription sub = SubscriptionRepository.INSTANCE.activeByStudent(student.getId());
        if (sub == null) {
            sub = SubscriptionRepository.INSTANCE.activeByCollege(student.college.getId());
        }
        if (sub == null) return 0;
        return sub.totalCredits - sub.usedCredits;
    }

    /**
     * Get transaction history for a subscription.
     */
    public List<CreditTransaction> getHistory(Long subscriptionId) {
        return CreditTransactionRepository.INSTANCE.bySubscription(subscriptionId);
    }

    /**
     * Get transaction history for a subscription (limited).
     */
    public List<CreditTransaction> getHistory(Long subscriptionId, int limit) {
        return CreditTransactionRepository.INSTANCE.bySubscription(subscriptionId, limit);
    }

    private void logTransaction(Subscription sub, Student student, CreditTransactionType type,
                                int amount, int balanceAfter, String description, String paymentRef) {
        CreditTransaction txn = new CreditTransaction();
        txn.subscription = sub;
        txn.student = student;
        txn.college = sub.college;
        txn.type = type;
        txn.amount = amount;
        txn.balanceAfter = balanceAfter;
        txn.description = description;
        txn.paymentReference = paymentRef;
        txn.save();
    }
}

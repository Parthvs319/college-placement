package models.enums;

/**
 * Types of credit transactions on a subscription.
 */
public enum CreditTransactionType {
    // Debit (usage)
    AI_RESUME_SCORE,
    AI_SUGGESTION,
    AI_MOCK_INTERVIEW,
    AI_COVER_LETTER,
    AI_JOB_MATCH,
    AI_OTHER,

    // Credit (top-up)
    PAYMENT_TOPUP,
    ADMIN_TOPUP,
    BONUS,
    REFUND
}

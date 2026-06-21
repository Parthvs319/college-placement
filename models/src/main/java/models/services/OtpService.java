package models.services;

import java.security.SecureRandom;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OTP service for verifying email addresses and phone numbers during college onboarding.
 *
 * Key  : "email:{address}" or "phone:{number}"
 * OTP  : 6-digit numeric
 * TTL  : 10 minutes
 * Limit: 5 wrong attempts before OTP is invalidated
 */
public enum OtpService {

    INSTANCE;

    private static final long OTP_TTL_MS   = 10 * 60 * 1000L;
    private static final int  MAX_ATTEMPTS = 5;

    private static class OtpEntry {
        final String otp;
        final long   expiresAt;
        int          attempts = 0;

        OtpEntry(String otp) {
            this.otp       = otp;
            this.expiresAt = System.currentTimeMillis() + OTP_TTL_MS;
        }

        boolean isExpired()   { return System.currentTimeMillis() > expiresAt; }
        boolean isExhausted() { return attempts >= MAX_ATTEMPTS; }
    }

    private final ConcurrentHashMap<String, OtpEntry> store = new ConcurrentHashMap<>();
    private final SecureRandom rng = new SecureRandom();

    // ── Public API ────────────────────────────────────────────────────

    /**
     * Generate a new 6-digit OTP for the given type + value.
     * Replaces any previously issued OTP for that identifier.
     *
     * @param type  "email" or "phone"
     * @param value the email address or phone number (will be lowercased/trimmed)
     * @return the generated OTP — caller is responsible for sending it
     */
    public String generate(String type, String value) {
        String otp = String.format("%06d", rng.nextInt(1_000_000));
        store.put(key(type, value), new OtpEntry(otp));
        System.out.println("[OTP-DEV] " + type + ":" + value + " → " + otp);
        return otp;
    }

    /**
     * Verify an OTP submitted by the user.
     */
    public VerifyResult verify(String type, String value, String submitted) {
        String   key   = key(type, value);
        OtpEntry entry = store.get(key);

        if (entry == null)       return VerifyResult.NOT_FOUND;
        if (entry.isExpired())   { store.remove(key); return VerifyResult.EXPIRED; }
        if (entry.isExhausted()) { store.remove(key); return VerifyResult.EXHAUSTED; }

        entry.attempts++;

        if (!entry.otp.equals(submitted == null ? "" : submitted.trim())) {
            return VerifyResult.WRONG_OTP;
        }

        store.remove(key); // one-time use
        return VerifyResult.SUCCESS;
    }

    public enum VerifyResult {
        SUCCESS, WRONG_OTP, EXPIRED, NOT_FOUND, EXHAUSTED;

        public boolean isSuccess() { return this == SUCCESS; }

        public String message() {
            return switch (this) {
                case SUCCESS   -> "Verified successfully";
                case WRONG_OTP -> "Invalid OTP. Please try again.";
                case EXPIRED   -> "OTP has expired. Please request a new one.";
                case NOT_FOUND -> "No OTP found. Please request a new one.";
                case EXHAUSTED -> "Too many wrong attempts. Please request a new OTP.";
            };
        }
    }

    // ── Legacy API (kept for backward compatibility) ──────────────────

    /** @deprecated Use generate(type, value) instead */
    public String sendOtp(String mobile) { return generate("phone", mobile); }

    /** @deprecated Use verify(type, value, otp) instead */
    public boolean verifyOtp(String mobile, String otp) {
        return verify("phone", mobile, otp).isSuccess();
    }

    // ── Internal ──────────────────────────────────────────────────────

    private String key(String type, String value) {
        return type.toLowerCase() + ":" + value.trim().toLowerCase();
    }
}

package superadmin;

import helpers.annotations.SuperAdminRole;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.superadmin.SuperAdminAccessMiddleware;
import models.body.SuperAdminLoginRequest;
import models.services.EmailService;
import models.services.OtpService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * POST /admin/send-otp
 *
 * Generates a 6-digit OTP and delivers it to the given email or phone number.
 * Used during college onboarding to verify contact details before saving.
 *
 * Request body: { "type": "email" | "phone", "value": "..." }
 * Response:     { "success": true, "message": "OTP sent to ..." }
 */
@SuperAdminRole
public enum SendOtpController implements BaseController {

    INSTANCE;

    @Override
    public void handle(RoutingContext event) {
        SuperAdminAccessMiddleware.INSTANCE.with(event, new ArrayList<>(), this.getClass())
                .flatMap(req -> sendOtp(req, event))
                .subscribe(
                        o -> ResponseUtils.INSTANCE.writeJsonResponse(event, o),
                        error -> ResponseUtils.INSTANCE.handleError(event, error)
                );
    }

    private rx.Single<Object> sendOtp(SuperAdminLoginRequest req, RoutingContext rc) {
        io.vertx.core.json.JsonObject body = rc.body().asJsonObject();
        if (body == null) throw new RoutingError("Request body is required");

        String type  = body.getString("type",  "").trim().toLowerCase();
        String value = body.getString("value", "").trim();

        if (!type.equals("email") && !type.equals("phone"))
            throw new RoutingError("type must be 'email' or 'phone'");
        if (value.isEmpty())
            throw new RoutingError("value is required");

        // Basic format validation
        if (type.equals("email") && !value.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))
            throw new RoutingError("Invalid email address");
        if (type.equals("phone") && !value.replaceAll("[\\s+\\-]", "").matches("[6-9]\\d{9}"))
            throw new RoutingError("Invalid Indian phone number (must be 10 digits starting with 6-9)");

        String otp = OtpService.INSTANCE.generate(type, value);

        // Both email and phone OTPs are delivered via email (Brevo).
        // For phone type the OTP email is sent to the TPO's contact email stored in the
        // request header "X-Contact-Email", or falls back to using the phone as a label.
        if (type.equals("email")) {
            return EmailService.sendEmail(value, "Your Applyra Verification OTP", buildEmailHtml(otp, "email"))
                    .map(sent -> buildResponse(sent, "email", maskEmail(value)));
        } else {
            // Phone OTP: send to the contact email supplied in the request body
            String contactEmail = rc.body().asJsonObject().getString("contactEmail", "").trim();
            if (contactEmail.isEmpty() || !contactEmail.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))
                throw new RoutingError("contactEmail is required to deliver phone OTP");
            return EmailService.sendEmail(contactEmail, "Your Applyra Phone Verification OTP", buildEmailHtml(otp, "phone"))
                    .map(sent -> buildResponse(sent, "phone", maskPhone(value)));
        }
    }

    private Object buildResponse(boolean sent, String type, String maskedValue) {
        Map<String, Object> out = new HashMap<>();
        out.put("success", sent);
        out.put("message", sent
                ? "OTP sent to " + maskedValue
                : "Failed to send OTP. Please try again.");
        return out;
    }

    // ── Email HTML ────────────────────────────────────────────────────

    private String buildEmailHtml(String otp, String type) {
        String subtitle = type.equals("phone")
                ? "Use this OTP to verify your phone number on Applyra."
                : "Use this OTP to verify your email address on Applyra.";
        return "<div style='font-family: Arial, sans-serif; max-width: 480px; margin: 0 auto; padding: 32px 24px;'>"
                + "<div style='text-align: center; margin-bottom: 28px;'>"
                + "<span style='font-size: 22px; font-weight: 800; letter-spacing: -0.5px;'>Applyra</span>"
                + "</div>"
                + "<div style='background: #f9fafb; border: 1px solid #e5e7eb; border-radius: 12px; padding: 28px; text-align: center;'>"
                + "<p style='color: #374151; font-size: 15px; margin: 0 0 20px;'>" + subtitle + "</p>"
                + "<div style='font-size: 40px; font-weight: 800; letter-spacing: 10px; color: #111827; margin: 0 0 20px;'>"
                + otp
                + "</div>"
                + "<p style='color: #6b7280; font-size: 13px; margin: 0;'>Valid for <strong>10 minutes</strong>. Do not share this with anyone.</p>"
                + "</div>"
                + "<p style='color: #9ca3af; font-size: 12px; text-align: center; margin-top: 20px;'>"
                + "This OTP was requested as part of college onboarding on Applyra."
                + "</p>"
                + "</div>";
    }

    // ── Masking helpers ───────────────────────────────────────────────

    private String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 2) return "***" + email.substring(at);
        return email.charAt(0) + "***" + email.substring(at - 1);
    }

    private String maskPhone(String phone) {
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.length() < 4) return "****";
        return "****" + digits.substring(digits.length() - 4);
    }
}

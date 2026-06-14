package models.services;

import rx.Single;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Email Service using Brevo (Sendinblue) HTTP API.
 * 300 emails/day free, no domain verification required.
 *
 * Configure via environment variables:
 * - BREVO_API_KEY: Brevo API key (from SMTP & API → API Keys tab)
 * - BREVO_FROM_EMAIL: Sender email (must match your Brevo account email)
 * - BREVO_FROM_NAME: Sender display name (default "Applyra")
 */
public class EmailService {

    private static final String BREVO_API_KEY = System.getenv().getOrDefault("BREVO_API_KEY", "");
    private static final String BREVO_FROM_EMAIL = System.getenv().getOrDefault("BREVO_FROM_EMAIL", "parthvs319@gmail.com");
    private static final String BREVO_FROM_NAME = System.getenv().getOrDefault("BREVO_FROM_NAME", "Applyra");
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    /**
     * Send an HTML email to a single recipient via Brevo API.
     * Returns true on success. Falls back to console logging if no API key configured.
     */
    public static Single<Boolean> sendEmail(String toEmail, String subject, String htmlBody) {
        if (BREVO_API_KEY == null || BREVO_API_KEY.isEmpty()) {
            System.out.println("[Email-DEV] To: " + toEmail + " | Subject: " + subject);
            System.out.println("[Email-DEV] Body: " + htmlBody);
            return Single.just(true);
        }

        return Single.fromCallable(() -> {
            try {
                String json = "{"
                        + "\"sender\":{\"name\":\"" + escapeJson(BREVO_FROM_NAME) + "\",\"email\":\"" + escapeJson(BREVO_FROM_EMAIL) + "\"},"
                        + "\"to\":[{\"email\":\"" + escapeJson(toEmail) + "\"}],"
                        + "\"subject\":\"" + escapeJson(subject) + "\","
                        + "\"htmlContent\":\"" + escapeJson(htmlBody) + "\""
                        + "}";

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.brevo.com/v3/smtp/email"))
                        .header("api-key", BREVO_API_KEY)
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();

                HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 201) {
                    System.out.println("[Email] Sent to " + toEmail + ": " + subject + " | Response: " + response.body());
                    return true;
                } else {
                    System.err.println("[Email] Brevo API error (" + response.statusCode() + "): " + response.body());
                    return false;
                }
            } catch (Exception e) {
                System.err.println("[Email] Failed to send to " + toEmail + ": " + e.getMessage());
                return false;
            }
        });
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    // ── HTML Email Builders ──────────────────────────────────────────

    public static String buildDriveAnnouncementHtml(String companyName, String title,
                                                     String driveDate, String registrationDeadline,
                                                     String collegeName, String portalUrl) {
        return "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;'>"
                + "<h2 style='color: #2563eb;'>New Placement Drive</h2>"
                + "<p>A new placement drive has been announced at <strong>" + collegeName + "</strong>.</p>"
                + "<table style='width: 100%; border-collapse: collapse; margin: 16px 0;'>"
                + "<tr><td style='padding: 8px; border-bottom: 1px solid #e5e7eb;'><strong>Company</strong></td>"
                + "<td style='padding: 8px; border-bottom: 1px solid #e5e7eb;'>" + companyName + "</td></tr>"
                + "<tr><td style='padding: 8px; border-bottom: 1px solid #e5e7eb;'><strong>Role</strong></td>"
                + "<td style='padding: 8px; border-bottom: 1px solid #e5e7eb;'>" + title + "</td></tr>"
                + "<tr><td style='padding: 8px; border-bottom: 1px solid #e5e7eb;'><strong>Drive Date</strong></td>"
                + "<td style='padding: 8px; border-bottom: 1px solid #e5e7eb;'>" + driveDate + "</td></tr>"
                + "<tr><td style='padding: 8px; border-bottom: 1px solid #e5e7eb;'><strong>Register By</strong></td>"
                + "<td style='padding: 8px; border-bottom: 1px solid #e5e7eb;'>" + registrationDeadline + "</td></tr>"
                + "</table>"
                + "<p><a href='" + portalUrl + "' style='background: #2563eb; color: white; padding: 12px 24px; "
                + "text-decoration: none; border-radius: 6px; display: inline-block;'>Apply Now</a></p>"
                + "</div>";
    }

    public static String buildOfferNotificationHtml(String studentName, String companyName,
                                                     String designation, String ctc,
                                                     String deadline, String portalUrl) {
        return "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;'>"
                + "<h2 style='color: #16a34a;'>Congratulations, " + studentName + "!</h2>"
                + "<p>You have received a placement offer.</p>"
                + "<table style='width: 100%; border-collapse: collapse; margin: 16px 0;'>"
                + "<tr><td style='padding: 8px; border-bottom: 1px solid #e5e7eb;'><strong>Company</strong></td>"
                + "<td style='padding: 8px; border-bottom: 1px solid #e5e7eb;'>" + companyName + "</td></tr>"
                + "<tr><td style='padding: 8px; border-bottom: 1px solid #e5e7eb;'><strong>Role</strong></td>"
                + "<td style='padding: 8px; border-bottom: 1px solid #e5e7eb;'>" + designation + "</td></tr>"
                + "<tr><td style='padding: 8px; border-bottom: 1px solid #e5e7eb;'><strong>CTC</strong></td>"
                + "<td style='padding: 8px; border-bottom: 1px solid #e5e7eb;'>" + ctc + " LPA</td></tr>"
                + "<tr><td style='padding: 8px; border-bottom: 1px solid #e5e7eb;'><strong>Respond By</strong></td>"
                + "<td style='padding: 8px; border-bottom: 1px solid #e5e7eb;'>" + deadline + "</td></tr>"
                + "</table>"
                + "<p><a href='" + portalUrl + "' style='background: #16a34a; color: white; padding: 12px 24px; "
                + "text-decoration: none; border-radius: 6px; display: inline-block;'>View Offer</a></p>"
                + "</div>";
    }

    public static String buildDeadlineReminderHtml(String studentName, String companyName,
                                                    String title, String deadline, String portalUrl) {
        return "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;'>"
                + "<h2 style='color: #ea580c;'>Registration Closing Soon</h2>"
                + "<p>Hi " + studentName + ", don't miss this opportunity!</p>"
                + "<table style='width: 100%; border-collapse: collapse; margin: 16px 0;'>"
                + "<tr><td style='padding: 8px; border-bottom: 1px solid #e5e7eb;'><strong>Company</strong></td>"
                + "<td style='padding: 8px; border-bottom: 1px solid #e5e7eb;'>" + companyName + "</td></tr>"
                + "<tr><td style='padding: 8px; border-bottom: 1px solid #e5e7eb;'><strong>Role</strong></td>"
                + "<td style='padding: 8px; border-bottom: 1px solid #e5e7eb;'>" + title + "</td></tr>"
                + "<tr><td style='padding: 8px; border-bottom: 1px solid #e5e7eb;'><strong>Deadline</strong></td>"
                + "<td style='padding: 8px; border-bottom: 1px solid #e5e7eb;'>" + deadline + "</td></tr>"
                + "</table>"
                + "<p><a href='" + portalUrl + "' style='background: #ea580c; color: white; padding: 12px 24px; "
                + "text-decoration: none; border-radius: 6px; display: inline-block;'>Apply Now</a></p>"
                + "</div>";
    }

    public static String buildResultNotificationHtml(String studentName, String companyName,
                                                      String roundName, String result, String portalUrl) {
        String color = "PASSED".equalsIgnoreCase(result) ? "#16a34a" : "#dc2626";
        return "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;'>"
                + "<h2 style='color: " + color + ";'>Round Result</h2>"
                + "<p>Hi " + studentName + ", your result is out.</p>"
                + "<table style='width: 100%; border-collapse: collapse; margin: 16px 0;'>"
                + "<tr><td style='padding: 8px; border-bottom: 1px solid #e5e7eb;'><strong>Company</strong></td>"
                + "<td style='padding: 8px; border-bottom: 1px solid #e5e7eb;'>" + companyName + "</td></tr>"
                + "<tr><td style='padding: 8px; border-bottom: 1px solid #e5e7eb;'><strong>Round</strong></td>"
                + "<td style='padding: 8px; border-bottom: 1px solid #e5e7eb;'>" + roundName + "</td></tr>"
                + "<tr><td style='padding: 8px; border-bottom: 1px solid #e5e7eb;'><strong>Status</strong></td>"
                + "<td style='padding: 8px; border-bottom: 1px solid #e5e7eb; color: " + color + ";'><strong>"
                + result + "</strong></td></tr>"
                + "</table>"
                + "<p><a href='" + portalUrl + "' style='background: #2563eb; color: white; padding: 12px 24px; "
                + "text-decoration: none; border-radius: 6px; display: inline-block;'>View Details</a></p>"
                + "</div>";
    }

    public static String buildCollegeOnboardingHtml(String collegeName, String code,
                                                       String city, String state,
                                                       String portalUrl) {
        String location = (city != null && !city.isEmpty()) ? city + (state != null && !state.isEmpty() ? ", " + state : "") : (state != null ? state : "");
        return "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;'>"
                + "<h2 style='color: #2563eb;'>Welcome to Applyra!</h2>"
                + "<p>Your college has been registered on the Applyra Placement Platform.</p>"
                + "<table style='width: 100%; border-collapse: collapse; margin: 16px 0;'>"
                + "<tr><td style='padding: 8px; border-bottom: 1px solid #e5e7eb;'><strong>College</strong></td>"
                + "<td style='padding: 8px; border-bottom: 1px solid #e5e7eb;'>" + collegeName + "</td></tr>"
                + "<tr><td style='padding: 8px; border-bottom: 1px solid #e5e7eb;'><strong>Code</strong></td>"
                + "<td style='padding: 8px; border-bottom: 1px solid #e5e7eb;'>" + code + "</td></tr>"
                + (location.isEmpty() ? "" :
                   "<tr><td style='padding: 8px; border-bottom: 1px solid #e5e7eb;'><strong>Location</strong></td>"
                   + "<td style='padding: 8px; border-bottom: 1px solid #e5e7eb;'>" + location + "</td></tr>")
                + "</table>"
                + "<div style='background: #f0fdf4; border: 1px solid #bbf7d0; border-radius: 8px; padding: 16px; margin: 16px 0;'>"
                + "<p style='margin: 0; color: #166534;'><strong>What happens next?</strong></p>"
                + "<p style='margin: 8px 0 0; color: #166534;'>Our team will review and verify your college. "
                + "Once verified, a TPO account will be created and you'll receive login credentials to access the placement portal.</p>"
                + "</div>"
                + (portalUrl != null && !portalUrl.isEmpty() ?
                   "<p><a href='" + portalUrl + "' style='background: #2563eb; color: white; padding: 12px 24px; "
                   + "text-decoration: none; border-radius: 6px; display: inline-block;'>Visit Applyra</a></p>" : "")
                + "<hr style='border: none; border-top: 1px solid #e5e7eb; margin: 24px 0;'>"
                + "<p style='color: #6b7280; font-size: 12px;'>Sent from Applyra Placement Platform</p>"
                + "</div>";
    }

    public static String buildTpoCredentialsHtml(String collegeName, String email,
                                                      String password, String collegeCode) {
        return "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;'>"
                + "<h2 style='color: #2563eb;'>Your Applyra TPO Account is Ready!</h2>"
                + "<p>Your college <strong>" + collegeName + "</strong> has been verified on Applyra. "
                + "A TPO (Training & Placement Officer) account has been created for you.</p>"
                + "<div style='background: #eff6ff; border: 1px solid #bfdbfe; border-radius: 8px; padding: 20px; margin: 16px 0;'>"
                + "<p style='margin: 0 0 12px; font-weight: bold; color: #1e40af;'>Your Login Credentials</p>"
                + "<table style='width: 100%; border-collapse: collapse;'>"
                + "<tr><td style='padding: 6px 0; color: #6b7280;'>Email</td>"
                + "<td style='padding: 6px 0; font-weight: bold; color: #1e293b;'>" + email + "</td></tr>"
                + "<tr><td style='padding: 6px 0; color: #6b7280;'>Password</td>"
                + "<td style='padding: 6px 0; font-family: monospace; font-size: 16px; font-weight: bold; color: #1e293b; "
                + "background: #f1f5f9; padding: 4px 8px; border-radius: 4px;'>" + password + "</td></tr>"
                + "<tr><td style='padding: 6px 0; color: #6b7280;'>College Code</td>"
                + "<td style='padding: 6px 0; font-weight: bold; color: #1e293b;'>" + collegeCode + "</td></tr>"
                + "</table>"
                + "</div>"
                + "<div style='background: #fef3c7; border: 1px solid #fcd34d; border-radius: 8px; padding: 12px 16px; margin: 16px 0;'>"
                + "<p style='margin: 0; color: #92400e; font-size: 13px;'>"
                + "<strong>Important:</strong> Please change your password after your first login. "
                + "Do not share these credentials with anyone.</p>"
                + "</div>"
                + "<hr style='border: none; border-top: 1px solid #e5e7eb; margin: 24px 0;'>"
                + "<p style='color: #6b7280; font-size: 12px;'>Sent from Applyra Placement Platform</p>"
                + "</div>";
    }

    public static String buildStudentInviteHtml(String collegeName, String registerUrl) {
        return "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;'>"
                + "<h2 style='color: #2563eb;'>You're Invited to Applyra!</h2>"
                + "<p>Your college <strong>" + collegeName + "</strong> has invited you to join the Applyra Placement Platform.</p>"
                + "<p>Register now to access placement drives, track applications, and manage your profile.</p>"
                + "<div style='text-align: center; margin: 24px 0;'>"
                + "<a href='" + registerUrl + "' style='background: #2563eb; color: white; padding: 14px 32px; "
                + "text-decoration: none; border-radius: 8px; display: inline-block; font-weight: bold; font-size: 16px;'>Register Now</a>"
                + "</div>"
                + "<p style='color: #6b7280; font-size: 13px;'>This invite link expires in 7 days. "
                + "If you didn't expect this email, you can safely ignore it.</p>"
                + "<hr style='border: none; border-top: 1px solid #e5e7eb; margin: 24px 0;'>"
                + "<p style='color: #6b7280; font-size: 12px;'>Sent from Applyra Placement Platform</p>"
                + "</div>";
    }

    public static String buildStudentCredentialsHtml(String collegeName, String email,
                                                      String password, String enrollmentNumber) {
        return "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;'>"
                + "<h2 style='color: #2563eb;'>Your Applyra Student Account is Ready!</h2>"
                + "<p>Your college <strong>" + collegeName + "</strong> has registered you on the Applyra Placement Platform.</p>"
                + "<div style='background: #eff6ff; border: 1px solid #bfdbfe; border-radius: 8px; padding: 20px; margin: 16px 0;'>"
                + "<p style='margin: 0 0 12px; font-weight: bold; color: #1e40af;'>Your Login Credentials</p>"
                + "<table style='width: 100%; border-collapse: collapse;'>"
                + "<tr><td style='padding: 6px 0; color: #6b7280;'>Email</td>"
                + "<td style='padding: 6px 0; font-weight: bold; color: #1e293b;'>" + email + "</td></tr>"
                + "<tr><td style='padding: 6px 0; color: #6b7280;'>Password</td>"
                + "<td style='padding: 6px 0; font-family: monospace; font-size: 16px; font-weight: bold; color: #1e293b; "
                + "background: #f1f5f9; padding: 4px 8px; border-radius: 4px;'>" + password + "</td></tr>"
                + "<tr><td style='padding: 6px 0; color: #6b7280;'>Enrollment No.</td>"
                + "<td style='padding: 6px 0; font-weight: bold; color: #1e293b;'>" + enrollmentNumber + "</td></tr>"
                + "</table>"
                + "</div>"
                + "<div style='background: #fef3c7; border: 1px solid #fcd34d; border-radius: 8px; padding: 12px 16px; margin: 16px 0;'>"
                + "<p style='margin: 0; color: #92400e; font-size: 13px;'>"
                + "<strong>Important:</strong> Please change your password after your first login.</p>"
                + "</div>"
                + "<hr style='border: none; border-top: 1px solid #e5e7eb; margin: 24px 0;'>"
                + "<p style='color: #6b7280; font-size: 12px;'>Sent from Applyra Placement Platform</p>"
                + "</div>";
    }

    public static String buildCompanyCredentialsHtml(String companyName, String collegeName,
                                                      String email, String password) {
        return "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;'>"
                + "<h2 style='color: #2563eb;'>Your Applyra Company Account is Ready!</h2>"
                + "<p><strong>" + collegeName + "</strong> has onboarded your company <strong>" + companyName
                + "</strong> on the Applyra Placement Platform.</p>"
                + "<p>You can now post placement drives, review applications, and manage candidates.</p>"
                + "<div style='background: #eff6ff; border: 1px solid #bfdbfe; border-radius: 8px; padding: 20px; margin: 16px 0;'>"
                + "<p style='margin: 0 0 12px; font-weight: bold; color: #1e40af;'>Your Login Credentials</p>"
                + "<table style='width: 100%; border-collapse: collapse;'>"
                + "<tr><td style='padding: 6px 0; color: #6b7280;'>Email</td>"
                + "<td style='padding: 6px 0; font-weight: bold; color: #1e293b;'>" + email + "</td></tr>"
                + "<tr><td style='padding: 6px 0; color: #6b7280;'>Password</td>"
                + "<td style='padding: 6px 0; font-family: monospace; font-size: 16px; font-weight: bold; color: #1e293b; "
                + "background: #f1f5f9; padding: 4px 8px; border-radius: 4px;'>" + password + "</td></tr>"
                + "</table>"
                + "</div>"
                + "<div style='background: #fef3c7; border: 1px solid #fcd34d; border-radius: 8px; padding: 12px 16px; margin: 16px 0;'>"
                + "<p style='margin: 0; color: #92400e; font-size: 13px;'>"
                + "<strong>Important:</strong> Please change your password after your first login.</p>"
                + "</div>"
                + "<hr style='border: none; border-top: 1px solid #e5e7eb; margin: 24px 0;'>"
                + "<p style='color: #6b7280; font-size: 12px;'>Sent from Applyra Placement Platform</p>"
                + "</div>";
    }

    public static String buildCustomHtml(String recipientName, String subject, String body) {
        return "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;'>"
                + "<h2 style='color: #2563eb;'>" + subject + "</h2>"
                + "<p>Hi " + recipientName + ",</p>"
                + "<p>" + body.replace("\n", "<br>") + "</p>"
                + "<hr style='border: none; border-top: 1px solid #e5e7eb; margin: 24px 0;'>"
                + "<p style='color: #6b7280; font-size: 12px;'>Sent from the Placement Portal</p>"
                + "</div>";
    }
}

package models.services;

import rx.Single;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;

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

    /**
     * Send an HTML email to a single recipient with optional CC recipients.
     * Each element in ccList is a String[2] {email, name} pair.
     */
    public static Single<Boolean> sendEmailWithCc(
            String toEmail, String toName,
            String subject, String htmlBody,
            java.util.List<String[]> ccList) {

        if (BREVO_API_KEY == null || BREVO_API_KEY.isEmpty()) {
            System.out.println("[Email-DEV] To: " + toEmail + " CC: " + (ccList != null ? ccList.size() : 0) + " | Subject: " + subject);
            return Single.just(true);
        }

        return Single.fromCallable(() -> {
            try {
                StringBuilder toField = new StringBuilder("[{\"email\":\"" + escapeJson(toEmail) + "\"");
                if (toName != null && !toName.isBlank()) toField.append(",\"name\":\"").append(escapeJson(toName)).append("\"");
                toField.append("}]");

                StringBuilder ccField = new StringBuilder("[");
                if (ccList != null && !ccList.isEmpty()) {
                    boolean first = true;
                    for (String[] cc : ccList) {
                        if (cc == null || cc.length < 1 || cc[0] == null || cc[0].isBlank()) continue;
                        if (!first) ccField.append(",");
                        ccField.append("{\"email\":\"").append(escapeJson(cc[0])).append("\"");
                        if (cc.length > 1 && cc[1] != null && !cc[1].isBlank())
                            ccField.append(",\"name\":\"").append(escapeJson(cc[1])).append("\"");
                        ccField.append("}");
                        first = false;
                    }
                }
                ccField.append("]");

                String json = "{"
                        + "\"sender\":{\"name\":\"" + escapeJson(BREVO_FROM_NAME) + "\",\"email\":\"" + escapeJson(BREVO_FROM_EMAIL) + "\"},"
                        + "\"to\":" + toField + ","
                        + (ccList != null && !ccList.isEmpty() ? "\"cc\":" + ccField + "," : "")
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
                if (response.statusCode() == 201) return true;
                System.err.println("[Email] Brevo CC email error (" + response.statusCode() + "): " + response.body());
                return false;
            } catch (Exception e) {
                System.err.println("[Email] sendEmailWithCc failed to " + toEmail + ": " + e.getMessage());
                return false;
            }
        });
    }

    /**
     * Send an HTML email with a single file attachment via Brevo API.
     *
     * @param attachmentBytes  raw bytes of the file to attach (e.g. PDF)
     * @param attachmentName   filename shown to recipient (e.g. "contract.pdf")
     */
    public static Single<Boolean> sendEmailWithAttachment(
            String toEmail, String subject, String htmlBody,
            byte[] attachmentBytes, String attachmentName) {

        if (BREVO_API_KEY == null || BREVO_API_KEY.isEmpty()) {
            System.out.println("[Email-DEV] To: " + toEmail + " | Subject: " + subject + " | Attachment: " + attachmentName);
            return Single.just(true);
        }

        return Single.fromCallable(() -> {
            try {
                String base64Content = Base64.getEncoder().encodeToString(attachmentBytes);
                String json = "{"
                        + "\"sender\":{\"name\":\"" + escapeJson(BREVO_FROM_NAME) + "\",\"email\":\"" + escapeJson(BREVO_FROM_EMAIL) + "\"},"
                        + "\"to\":[{\"email\":\"" + escapeJson(toEmail) + "\"}],"
                        + "\"subject\":\"" + escapeJson(subject) + "\","
                        + "\"htmlContent\":\"" + escapeJson(htmlBody) + "\","
                        + "\"attachment\":[{\"content\":\"" + base64Content + "\",\"name\":\"" + escapeJson(attachmentName) + "\"}]"
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
                    System.out.println("[Email] Sent (with attachment) to " + toEmail + ": " + subject);
                    return true;
                } else {
                    System.err.println("[Email] Brevo attachment email error (" + response.statusCode() + "): " + response.body());
                    return false;
                }
            } catch (Exception e) {
                System.err.println("[Email] Failed to send attachment email to " + toEmail + ": " + e.getMessage());
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

    /**
     * Email sent to the college admin when a contract is uploaded and a TPO account is created.
     * Includes contract download link + login credentials.
     */
    public static String buildContractWithCredentialsHtml(
            String tpoName, String collegeName, String collegeCode,
            String email, String password,
            String contractDownloadUrl, String validFrom, String validTo,
            String portalUrl) {
        return buildContractWithCredentialsHtml(tpoName, collegeName, collegeCode,
                email, password, contractDownloadUrl, validFrom, validTo, portalUrl,
                null, "PAID");
    }

    /**
     * Full version — includes contract amount, type, and proper login URL.
     *
     * @param contractAmount  formatted amount string e.g. "₹1,50,000" or "Free Trial"
     * @param contractType    "PAID" or "FREE_TRIAL"
     */
    public static String buildContractWithCredentialsHtml(
            String tpoName, String collegeName, String collegeCode,
            String email, String password,
            String contractDownloadUrl, String validFrom, String validTo,
            String portalUrl, String contractAmount, String contractType) {

        boolean isFreeTrial = "FREE_TRIAL".equalsIgnoreCase(contractType);
        String loginUrl = (portalUrl != null && !portalUrl.isEmpty())
                ? (portalUrl.endsWith("/") ? portalUrl + "login" : portalUrl + "/login")
                : "";

        return "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 0 0 24px;'>"

                // Header
                + "<div style='background:#1a73e8; padding:28px 32px; border-radius:10px 10px 0 0;'>"
                + "<h1 style='color:white; margin:0; font-size:22px; font-weight:800; letter-spacing:-0.5px;'>Applyra</h1>"
                + "<p style='color:rgba(255,255,255,0.85); margin:6px 0 0; font-size:13px;'>Placement Intelligence Platform</p>"
                + "</div>"

                // Body
                + "<div style='padding:28px 32px; background:#ffffff; border:1px solid #e5e7eb; border-top:none;'>"
                + "<h2 style='color:#111827; font-size:18px; margin:0 0 8px;'>Welcome to Applyra!</h2>"
                + "<p style='color:#374151; font-size:14px; margin:0 0 20px;'>Hi <strong>" + escapeJson(tpoName) + "</strong>, your institution "
                + "<strong>" + escapeJson(collegeName) + "</strong> has been formally onboarded on Applyra.</p>"

                // Contract details box
                + "<div style='background:#f0f7ff; border:1px solid #bfdbfe; border-radius:8px; padding:20px; margin:0 0 20px;'>"
                + "<p style='margin:0 0 12px; font-weight:700; color:#1e40af; font-size:14px;'>📄 Contract Details</p>"
                + "<table style='width:100%; border-collapse:collapse;'>"
                + "<tr><td style='padding:5px 0; color:#6b7280; font-size:13px; width:130px;'>College</td>"
                + "<td style='padding:5px 0; font-weight:600; color:#1e293b; font-size:13px;'>" + escapeJson(collegeName) + " (" + escapeJson(collegeCode) + ")</td></tr>"
                + "<tr><td style='padding:5px 0; color:#6b7280; font-size:13px;'>Type</td>"
                + "<td style='padding:5px 0; font-size:13px;'><span style='background:" + (isFreeTrial ? "#fef3c7" : "#dcfce7") + "; color:" + (isFreeTrial ? "#92400e" : "#166534") + "; padding:2px 8px; border-radius:12px; font-weight:600; font-size:12px;'>"
                + (isFreeTrial ? "Free Trial" : "Paid") + "</span></td></tr>"
                + (contractAmount != null && !contractAmount.isBlank()
                    ? "<tr><td style='padding:5px 0; color:#6b7280; font-size:13px;'>Value</td>"
                    + "<td style='padding:5px 0; font-weight:600; color:#1e293b; font-size:13px;'>" + escapeJson(contractAmount) + "</td></tr>" : "")
                + (validFrom != null && !validFrom.isBlank()
                    ? "<tr><td style='padding:5px 0; color:#6b7280; font-size:13px;'>Valid From</td>"
                    + "<td style='padding:5px 0; font-weight:600; color:#1e293b; font-size:13px;'>" + escapeJson(validFrom) + "</td></tr>" : "")
                + (validTo != null && !validTo.isBlank()
                    ? "<tr><td style='padding:5px 0; color:#6b7280; font-size:13px;'>Valid Until</td>"
                    + "<td style='padding:5px 0; font-weight:600; color:#1e293b; font-size:13px;'>" + escapeJson(validTo) + "</td></tr>" : "")
                + "</table>"
                + (contractDownloadUrl != null && !contractDownloadUrl.isEmpty()
                    ? "<div style='margin-top:14px;'><a href='" + contractDownloadUrl + "' "
                    + "style='background:#1a73e8; color:white; padding:9px 18px; text-decoration:none; "
                    + "border-radius:6px; display:inline-block; font-size:13px; font-weight:600;'>⬇ Download Contract PDF</a></div>"
                    : "<p style='margin:12px 0 0; color:#6b7280; font-size:13px; font-style:italic;'>"
                    + "The signed contract will be shared once executed.</p>")
                + "</div>"

                // Login credentials box
                + "<div style='background:#eff6ff; border:1px solid #bfdbfe; border-radius:8px; padding:20px; margin:0 0 20px;'>"
                + "<p style='margin:0 0 12px; font-weight:700; color:#1e40af; font-size:14px;'>🔑 Your TPO Portal Login</p>"
                + "<table style='width:100%; border-collapse:collapse;'>"
                + "<tr><td style='padding:6px 0; color:#6b7280; font-size:13px; width:130px;'>Email</td>"
                + "<td style='padding:6px 0; font-weight:600; color:#1e293b; font-size:13px;'>" + escapeJson(email) + "</td></tr>"
                + "<tr><td style='padding:6px 0; color:#6b7280; font-size:13px;'>Password</td>"
                + "<td style='padding:6px 0; font-size:13px;'><span style='font-family:monospace; font-size:15px; font-weight:700; color:#1e293b; "
                + "background:#f1f5f9; padding:3px 10px; border-radius:4px; display:inline-block;'>" + escapeJson(password) + "</span></td></tr>"
                + "<tr><td style='padding:6px 0; color:#6b7280; font-size:13px;'>College Code</td>"
                + "<td style='padding:6px 0; font-weight:600; color:#1e293b; font-size:13px;'>" + escapeJson(collegeCode) + "</td></tr>"
                + "</table>"
                + "</div>"

                // Warning
                + "<div style='background:#fef3c7; border:1px solid #fcd34d; border-radius:8px; padding:12px 16px; margin:0 0 20px;'>"
                + "<p style='margin:0; color:#92400e; font-size:13px;'>"
                + "<strong>Important:</strong> Please change your password after your first login and keep your credentials confidential.</p>"
                + "</div>"

                // CTA button → login page
                + (!loginUrl.isEmpty()
                    ? "<p style='text-align:center; margin:0 0 8px;'><a href='" + loginUrl + "' "
                    + "style='background:#1a73e8; color:white; padding:13px 32px; text-decoration:none; "
                    + "border-radius:8px; display:inline-block; font-weight:700; font-size:15px; letter-spacing:0.3px;'>"
                    + "Login to Applyra →</a></p>"
                    + "<p style='text-align:center; margin:4px 0 0; font-size:11px; color:#9ca3af;'>" + loginUrl + "</p>"
                    : "")

                + "<hr style='border:none; border-top:1px solid #e5e7eb; margin:24px 0 16px;'>"
                + "<p style='color:#9ca3af; font-size:11px; text-align:center; margin:0;'>Applyra Placement Intelligence Platform · applyra.in</p>"
                + "</div>"
                + "</div>";
    }

    /**
     * Email sent to the college when an invoice is generated.
     */
    public static String buildInvoiceEmailHtml(
            String collegeName, String invoiceNumber,
            String billingPeriod, String amount,
            String invoiceDownloadUrl, String dueDate) {

        return "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;'>"
                + "<h2 style='color: #1a73e8;'>Invoice Generated — " + escapeJson(invoiceNumber) + "</h2>"
                + "<p>Dear <strong>" + escapeJson(collegeName) + "</strong>,</p>"
                + "<p>An invoice has been generated for your Applyra platform subscription.</p>"
                + "<div style='background:#f0f7ff; border:1px solid #bfdbfe; border-radius:8px; padding:20px; margin:20px 0;'>"
                + "<table style='width:100%; border-collapse:collapse;'>"
                + "<tr><td style='padding:6px 0; color:#6b7280;'>Invoice No.</td>"
                + "<td style='padding:6px 0; font-weight:bold; color:#1e293b;'>" + escapeJson(invoiceNumber) + "</td></tr>"
                + "<tr><td style='padding:6px 0; color:#6b7280;'>Billing Period</td>"
                + "<td style='padding:6px 0; font-weight:bold; color:#1e293b;'>" + escapeJson(billingPeriod) + "</td></tr>"
                + "<tr><td style='padding:6px 0; color:#6b7280;'>Amount Due</td>"
                + "<td style='padding:6px 0; font-weight:bold; color:#1e293b; font-size:16px;'>" + escapeJson(amount) + "</td></tr>"
                + "<tr><td style='padding:6px 0; color:#6b7280;'>Due Date</td>"
                + "<td style='padding:6px 0; font-weight:bold; color:#1e293b;'>" + escapeJson(dueDate) + "</td></tr>"
                + "</table>"
                + (invoiceDownloadUrl != null && !invoiceDownloadUrl.isEmpty()
                    ? "<div style='margin-top:16px;'><a href='" + invoiceDownloadUrl + "' "
                    + "style='background:#1a73e8; color:white; padding:10px 20px; text-decoration:none; "
                    + "border-radius:6px; display:inline-block;'>⬇ Download Invoice PDF</a></div>"
                    : "")
                + "</div>"
                + "<p style='color:#6b7280; font-size:13px;'>Please process this payment by the due date to avoid service interruption.</p>"
                + "<hr style='border:none; border-top:1px solid #e5e7eb; margin:24px 0;'>"
                + "<p style='color:#6b7280; font-size:12px;'>Sent from Applyra Placement Intelligence Platform</p>"
                + "</div>";
    }

    /**
     * Password reset OTP email.
     */
    public static String buildPasswordResetHtml(String name, String otp) {
        String displayName = (name != null && !name.isBlank()) ? escapeJson(name) : "there";
        return "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;'>"
                + "<h2 style='color: #1a73e8;'>Reset your password</h2>"
                + "<p>Hi " + displayName + ",</p>"
                + "<p>We received a request to reset your Applyra account password.</p>"
                + "<p>Use the OTP below. It expires in <strong>10 minutes</strong>.</p>"
                + "<div style='background:#f0f7ff; border:1px solid #bfdbfe; border-radius:8px; "
                + "padding:24px; text-align:center; margin:24px 0;'>"
                + "<div style='font-size:36px; font-weight:bold; letter-spacing:8px; color:#1a73e8;'>"
                + escapeJson(otp)
                + "</div>"
                + "</div>"
                + "<p style='color:#6b7280; font-size:13px;'>If you did not request this, you can safely ignore this email. "
                + "Your password will not change.</p>"
                + "<hr style='border:none; border-top:1px solid #e5e7eb; margin:24px 0;'>"
                + "<p style='color:#6b7280; font-size:12px;'>Applyra Placement Intelligence Platform</p>"
                + "</div>";
    }
}

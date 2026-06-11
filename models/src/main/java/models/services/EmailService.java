package models.services;

import rx.Single;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

/**
 * Email Service for sending placement notifications via SMTP.
 * Configure via environment variables:
 * - SMTP_HOST: SMTP server (e.g. smtp.gmail.com)
 * - SMTP_PORT: SMTP port (default 587)
 * - SMTP_USERNAME: SMTP auth username
 * - SMTP_PASSWORD: SMTP auth password / app password
 * - SMTP_FROM_EMAIL: Sender email address
 * - SMTP_FROM_NAME: Sender display name (default "Placement Portal")
 */
public class EmailService {

    private static final String SMTP_HOST = System.getenv().getOrDefault("SMTP_HOST", "");
    private static final String SMTP_PORT = System.getenv().getOrDefault("SMTP_PORT", "587");
    private static final String SMTP_USERNAME = System.getenv().getOrDefault("SMTP_USERNAME", "");
    private static final String SMTP_PASSWORD = System.getenv().getOrDefault("SMTP_PASSWORD", "");
    private static final String SMTP_FROM_EMAIL = System.getenv().getOrDefault("SMTP_FROM_EMAIL", "noreply@placement.edu");
    private static final String SMTP_FROM_NAME = System.getenv().getOrDefault("SMTP_FROM_NAME", "Placement Portal");

    /**
     * Send an HTML email to a single recipient.
     * Returns true on success. Falls back to console logging in dev mode.
     */
    public static Single<Boolean> sendEmail(String toEmail, String subject, String htmlBody) {
        if (SMTP_HOST == null || SMTP_HOST.isEmpty()) {
            System.out.println("[Email-DEV] To: " + toEmail + " | Subject: " + subject);
            System.out.println("[Email-DEV] Body: " + htmlBody);
            return Single.just(true);
        }

        return Single.fromCallable(() -> {
            try {
                Properties props = new Properties();
                props.put("mail.smtp.host", SMTP_HOST);
                props.put("mail.smtp.port", SMTP_PORT);
                props.put("mail.smtp.auth", "true");
                props.put("mail.smtp.starttls.enable", "true");

                Session session = Session.getInstance(props, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(SMTP_USERNAME, SMTP_PASSWORD);
                    }
                });

                MimeMessage message = new MimeMessage(session);
                message.setFrom(new InternetAddress(SMTP_FROM_EMAIL, SMTP_FROM_NAME));
                message.addRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
                message.setSubject(subject);
                message.setContent(htmlBody, "text/html; charset=utf-8");

                Transport.send(message);
                System.out.println("[Email] Sent to " + toEmail + ": " + subject);
                return true;
            } catch (Exception e) {
                System.err.println("[Email] Failed to send to " + toEmail + ": " + e.getMessage());
                return false;
            }
        });
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

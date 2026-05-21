package models.services;

import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.Vertx;
import rx.Single;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * WhatsApp Service for sending placement notifications via WhatsApp Business API.
 * Configure via environment variables:
 * - WHATSAPP_API_URL: Base URL for WhatsApp API (Meta Cloud API or Twilio)
 * - WHATSAPP_API_KEY: Bearer token for authentication
 * - WHATSAPP_PHONE_NUMBER_ID: Phone number ID (WhatsApp Business API)
 */
public class WhatsAppService {

    private static final String WHATSAPP_API_URL = System.getenv().getOrDefault("WHATSAPP_API_URL", "");
    private static final String WHATSAPP_API_KEY = System.getenv().getOrDefault("WHATSAPP_API_KEY", "");
    private static final String WHATSAPP_PHONE_NUMBER_ID = System.getenv().getOrDefault("WHATSAPP_PHONE_NUMBER_ID", "");

    private static Vertx vertxInstance;

    public static void initialize(Vertx vertx) {
        vertxInstance = vertx;
    }

    /**
     * Send a WhatsApp text message to a phone number.
     * Returns true if delivered (or if in dev mode with no API configured).
     */
    public static Single<Boolean> sendMessage(String toPhoneNumber, String message) {
        if (WHATSAPP_API_URL == null || WHATSAPP_API_URL.isEmpty()) {
            System.out.println("[WhatsApp-DEV] Would send to " + toPhoneNumber + ": " + message);
            return Single.just(true);
        }

        return Single.fromCallable(() -> {
            try {
                String formattedPhone = formatPhoneNumber(toPhoneNumber);
                JsonObject body = buildRequestBody(formattedPhone, message);

                URL url = new URL(WHATSAPP_API_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Authorization", "Bearer " + WHATSAPP_API_KEY);
                conn.setDoOutput(true);

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = body.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();
                boolean success = responseCode >= 200 && responseCode < 300;

                if (!success) {
                    System.err.println("[WhatsApp] API error: " + responseCode + " - " + conn.getResponseMessage());
                }

                conn.disconnect();
                return success;
            } catch (Exception e) {
                System.err.println("[WhatsApp] Error sending message: " + e.getMessage());
                return false;
            }
        });
    }

    // ── Placement Message Builders ───────────────────────────────────

    public static String buildDriveAnnouncement(String companyName, String title, String driveDate,
                                                 String registrationDeadline, String collegeName) {
        return "New Placement Drive!\n\n"
                + "Company: " + companyName + "\n"
                + "Role: " + title + "\n"
                + "Drive Date: " + driveDate + "\n"
                + "Register By: " + registrationDeadline + "\n"
                + "College: " + collegeName + "\n\n"
                + "Log in to the placement portal to apply.";
    }

    public static String buildOfferNotification(String studentName, String companyName,
                                                 String designation, String ctc, String deadline) {
        return "Congratulations " + studentName + "!\n\n"
                + "You have received an offer:\n"
                + "Company: " + companyName + "\n"
                + "Role: " + designation + "\n"
                + "CTC: " + ctc + " LPA\n"
                + "Respond By: " + deadline + "\n\n"
                + "Log in to accept or decline.";
    }

    public static String buildDeadlineReminder(String studentName, String companyName,
                                                String title, String deadline) {
        return "Reminder for " + studentName + "!\n\n"
                + "Registration closes soon:\n"
                + "Company: " + companyName + "\n"
                + "Role: " + title + "\n"
                + "Deadline: " + deadline + "\n\n"
                + "Apply now on the placement portal.";
    }

    public static String buildResultNotification(String studentName, String companyName,
                                                  String roundName, String result) {
        return "Hi " + studentName + ",\n\n"
                + "Your result for " + companyName + " - " + roundName + ":\n"
                + "Status: " + result + "\n\n"
                + "Check the portal for details.";
    }

    public static String buildCustomMessage(String recipientName, String subject, String body) {
        return "Hi " + recipientName + ",\n\n"
                + subject + "\n\n"
                + body;
    }

    // ── Internal Helpers ─────────────────────────────────────────────

    private static JsonObject buildRequestBody(String toPhone, String message) {
        JsonObject body = new JsonObject();
        if (!WHATSAPP_PHONE_NUMBER_ID.isEmpty()) {
            body.put("messaging_product", "whatsapp");
            body.put("to", toPhone);
            body.put("type", "text");
            body.put("text", new JsonObject().put("body", message));
        } else {
            body.put("to", toPhone);
            body.put("message", message);
        }
        return body;
    }

    private static String formatPhoneNumber(String phone) {
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.length() == 10) {
            return "91" + digits;  // Indian number
        }
        return digits;
    }
}

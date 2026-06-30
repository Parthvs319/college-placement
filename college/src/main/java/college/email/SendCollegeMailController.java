package college.email;

import helpers.annotations.CollegeRole;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.college.CollegeAccessMiddleware;
import models.body.CollegeLoginRequest;
import models.services.EmailService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * POST /college/send-mail
 *
 * Compose and send a personalised email from the college TPO to selected
 * users (students, company HRs, team members). Each recipient gets their
 * own individual email.
 *
 * Body (JSON):
 * {
 *   "recipients": [ {"email":"a@b.com","name":"Alice"}, ... ],
 *   "cc":         [ {"email":"tpo@college.edu","name":"TPO"} ],
 *   "subject":    "Subject line",
 *   "body":       "Hi {{name}},\n\nYour message here."
 * }
 *
 * In the body text, {{name}} is replaced with each recipient's first name.
 */
@CollegeRole
public enum SendCollegeMailController implements BaseController {

    INSTANCE;

    @Override
    public void handle(RoutingContext event) {
        CollegeAccessMiddleware.INSTANCE.with(event, new ArrayList<>(), this.getClass())
                .map(req -> dispatch(req, event))
                .subscribe(
                        o -> ResponseUtils.INSTANCE.writeJsonResponse(event, o),
                        error -> ResponseUtils.INSTANCE.handleError(event, error)
                );
    }

    private Object dispatch(CollegeLoginRequest request, RoutingContext rc) {
        JsonObject body = rc.body().asJsonObject();
        if (body == null) throw new RoutingError("Request body is required");

        String collegeName = request.getCollege().getName();

        // Get the TPO's email and name for replyTo
        String tpoEmail = request.getUser().getEmail();
        String tpoName  = request.getUser().getName();

        // Parse recipients
        JsonArray recipientsJson = body.getJsonArray("recipients");
        if (recipientsJson == null || recipientsJson.isEmpty())
            throw new RoutingError("At least one recipient is required");

        List<String[]> recipients = new ArrayList<>();
        for (int i = 0; i < recipientsJson.size(); i++) {
            JsonObject r = recipientsJson.getJsonObject(i);
            String email = r.getString("email", "").trim();
            String name  = r.getString("name",  "").trim();
            if (!email.isBlank()) recipients.add(new String[]{email, name});
        }
        if (recipients.isEmpty()) throw new RoutingError("No valid recipient emails provided");

        // Parse CC
        List<String[]> ccList = new ArrayList<>();
        JsonArray ccJson = body.getJsonArray("cc");
        if (ccJson != null) {
            for (int i = 0; i < ccJson.size(); i++) {
                JsonObject c = ccJson.getJsonObject(i);
                String email = c.getString("email", "").trim();
                String name  = c.getString("name",  "").trim();
                if (!email.isBlank()) ccList.add(new String[]{email, name});
            }
        }

        // Parse subject + body
        String subject = body.getString("subject", "").trim();
        if (subject.isBlank()) throw new RoutingError("Subject is required");
        String bodyText = body.getString("body", "").trim();
        if (bodyText.isBlank()) throw new RoutingError("Email body is required");

        int total = recipients.size();
        final String fSubject  = subject;
        final String fBodyText = bodyText;
        final String fCollegeName = collegeName != null ? collegeName : "Your College";
        final String fTpoEmail = tpoEmail;
        final String fTpoName  = tpoName;

        // Send asynchronously with TPO as replyTo
        new Thread(() -> {
            AtomicInteger sent   = new AtomicInteger();
            AtomicInteger failed = new AtomicInteger();

            for (String[] recipient : recipients) {
                String toEmail = recipient[0];
                String toName  = recipient[1];
                String firstName = extractFirstName(toName);

                String personalised = fBodyText.replace("{{name}}", firstName != null ? firstName : toName);

                String html = buildCollegeMailHtml(
                        toName.isBlank() ? firstName : toName,
                        fSubject, personalised, fCollegeName
                );

                try {
                    EmailService.sendEmailWithReplyTo(
                            toEmail, toName, fSubject, html,
                            fTpoEmail, fTpoName, ccList
                    ).subscribe(
                            ok -> { if (Boolean.TRUE.equals(ok)) sent.getAndIncrement(); else failed.getAndIncrement(); },
                            err -> failed.getAndIncrement()
                    );
                    Thread.sleep(100);
                } catch (Exception e) {
                    failed.getAndIncrement();
                    System.err.println("[CollegeMail] Error sending to " + toEmail + ": " + e.getMessage());
                }
            }

            System.out.println("[CollegeMail] Done - sent=" + sent + " failed=" + failed
                    + " total=" + total + " college=" + fCollegeName + " replyTo=" + fTpoEmail
                    + (ccList.isEmpty() ? "" : " cc=" + ccList.size()));
        }, "college-mail-blast").start();

        Map<String, Object> result = new HashMap<>();
        result.put("total",   total);
        result.put("message", "Emails queued for " + total + " recipient" + (total == 1 ? "" : "s") + ". Sending in background.");
        return result;
    }

    private String extractFirstName(String fullName) {
        if (fullName == null || fullName.isBlank()) return null;
        String[] parts = fullName.trim().split("\\s+");
        String first = parts[0];
        return Character.toUpperCase(first.charAt(0)) + first.substring(1).toLowerCase();
    }

    private String buildCollegeMailHtml(String recipientName, String subject, String bodyText, String collegeName) {
        String greeting = (recipientName != null && !recipientName.isBlank())
                ? "Hi " + recipientName + ","
                : "Hello,";
        String htmlBody = bodyText
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\n", "<br>");

        return "<!DOCTYPE html>"
            + "<html><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width,initial-scale=1'></head>"
            + "<body style='margin:0;padding:0;background:#F3F4F6;font-family:-apple-system,BlinkMacSystemFont,\"Segoe UI\",Roboto,Arial,sans-serif'>"
            + "<table width='100%' cellpadding='0' cellspacing='0' style='background:#F3F4F6;padding:28px 16px'>"
            + "<tr><td align='center'>"
            + "<table width='600' cellpadding='0' cellspacing='0' style='max-width:600px;width:100%'>"
            + "<tr><td style='background:#1A1A2E;border-radius:12px 12px 0 0;padding:24px 32px;'>"
            + "<span style='color:#C4A55A;font-size:20px;font-weight:800;letter-spacing:-0.3px'>Applyra</span>"
            + "<span style='color:#9CA3AF;font-size:12px;margin-left:10px'>" + escapeHtml(collegeName) + "</span>"
            + "</td></tr>"
            + "<tr><td style='background:#FFFFFF;padding:32px;border:1px solid #E5E7EB;border-top:none'>"
            + "<p style='margin:0 0 16px;font-size:15px;font-weight:600;color:#111827'>" + greeting + "</p>"
            + "<div style='font-size:14px;line-height:1.7;color:#374151'>" + htmlBody + "</div>"
            + "</td></tr>"
            + "<tr><td style='background:#F9FAFB;border-radius:0 0 12px 12px;border:1px solid #E5E7EB;border-top:none;padding:16px 32px;text-align:center'>"
            + "<p style='margin:0;color:#9CA3AF;font-size:11px'>Sent via Applyra on behalf of " + escapeHtml(collegeName) + " Placement Cell</p>"
            + "</td></tr>"
            + "</table>"
            + "</td></tr></table>"
            + "</body></html>";
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}

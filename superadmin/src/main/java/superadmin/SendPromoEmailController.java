package superadmin;

import helpers.annotations.SuperAdminRole;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.superadmin.SuperAdminAccessMiddleware;
import models.body.SuperAdminLoginRequest;
import models.repos.StudentRepository;
import models.services.EmailService;
import models.sql.Student;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * POST /admin/send-promo-email
 *
 * Sends a promotional email about Applyra's AI features to unplaced students.
 *
 * Optional body:
 *   { "collegeId": 42 }   — restrict to a single college's unplaced students
 *                           omit to target all unplaced students platform-wide
 *
 * Response:
 *   { total, sent, failed, message }
 */
@SuperAdminRole
public enum SendPromoEmailController implements BaseController {

    INSTANCE;

    private static final String APPLYRA_LOGO_URL = "https://applyra.in/logo.png";

    @Override
    public void handle(RoutingContext event) {
        SuperAdminAccessMiddleware.INSTANCE.with(event, new ArrayList<>(), this.getClass())
                .map(req -> startSend(req, event))
                .subscribe(
                        o -> ResponseUtils.INSTANCE.writeJsonResponse(event, o),
                        error -> ResponseUtils.INSTANCE.handleError(event, error)
                );
    }

    private Object startSend(SuperAdminLoginRequest req, RoutingContext rc) {
        // Optional college filter
        String collegeIdStr = null;
        try {
            var body = rc.body().asJsonObject();
            if (body != null) collegeIdStr = body.getString("collegeId");
        } catch (Exception ignored) {}

        // Collect unplaced students
        List<Student> unplaced;
        if (collegeIdStr != null && !collegeIdStr.isBlank()) {
            long cid = Long.parseLong(collegeIdStr);
            unplaced = StudentRepository.INSTANCE.findUnplaced(cid);
        } else {
            // Platform-wide: all unplaced, non-opted-out students
            unplaced = StudentRepository.INSTANCE.where()
                    .eq("placed", false)
                    .eq("optedOut", false)
                    .findList();
        }

        // Deduplicate by email (safety net)
        List<Student> targets = unplaced.stream()
                .filter(s -> s.getUser() != null && s.getUser().getEmail() != null && !s.getUser().getEmail().isBlank())
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(
                                s -> s.getUser().getEmail().toLowerCase(),
                                s -> s,
                                (a, b) -> a
                        ),
                        m -> new ArrayList<>(m.values())
                ));

        int total = targets.size();

        // Fire off emails asynchronously — respond immediately with count
        new Thread(() -> {
            AtomicInteger sent = new AtomicInteger();
            AtomicInteger failed = new AtomicInteger();
            for (Student student : targets) {
                try {
                    String email     = student.getUser().getEmail();
                    String firstName = extractFirstName(student.getUser().getName());
                    String college   = student.getCollege() != null ? student.getCollege().getName() : null;
                    String html      = buildPromoHtml(firstName, college);
                    String subject   = firstName != null
                            ? firstName + ", land your dream job with Applyra AI 🚀"
                            : "Land your dream job with Applyra AI 🚀";

                    EmailService.sendEmail(email, subject, html)
                            .subscribe(success -> {
                                if (Boolean.TRUE.equals(success)) {
                                    sent.getAndIncrement();
                                } else {
                                    failed.getAndIncrement();
                                }
                            }, throwable -> {
                                failed.getAndIncrement();
                            });
                    // Small rate-limit buffer
                    Thread.sleep(120);
                } catch (Exception e) {
                    failed.getAndIncrement();
                    System.err.println("[SendPromoEmail] Error for student " + student.getId() + ": " + e.getMessage());
                }
            }
            System.out.println("[SendPromoEmail] Done — sent=" + sent + " failed=" + failed + " total=" + total);
        }, "promo-email-blast").start();

        Map<String, Object> result = new HashMap<>();
        result.put("total",   total);
        result.put("message", "Promo emails queued for " + total + " unplaced students. Sending in background.");
        return result;
    }

    private String extractFirstName(String fullName) {
        if (fullName == null || fullName.isBlank()) return null;
        String[] parts = fullName.trim().split("\\s+");
        String first = parts[0];
        // Capitalise first letter
        return Character.toUpperCase(first.charAt(0)) + first.substring(1).toLowerCase();
    }

    // ── Email HTML ──────────────────────────────────────────────────────────

    private String buildPromoHtml(String firstName, String collegeName) {
        String greeting = firstName != null ? "Hi " + firstName + "," : "Hi there,";
        String collegeNote = collegeName != null
                ? "<p style='margin:0 0 20px;color:#6B7280;font-size:14px;'>We noticed you're a student at <strong style='color:#374151'>" + collegeName + "</strong> and are still exploring opportunities. We're here to help.</p>"
                : "";

        return "<!DOCTYPE html>" +
            "<html><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width,initial-scale=1'>" +
            "<title>Land your dream job with Applyra</title></head>" +
            "<body style='margin:0;padding:0;font-family:-apple-system,BlinkMacSystemFont,\"Segoe UI\",Roboto,\"Helvetica Neue\",Arial,sans-serif;background:#F3F4F6'>" +

            // Wrapper
            "<table width='100%' cellpadding='0' cellspacing='0' style='background:#F3F4F6;padding:32px 16px'>" +
            "<tr><td align='center'>" +
            "<table width='600' cellpadding='0' cellspacing='0' style='max-width:600px;width:100%'>" +

            // ── Header ───────────────────────────────────────────────────────
            "<tr><td style='background:linear-gradient(135deg,#1A1A2E 0%,#16213E 50%,#0F3460 100%);border-radius:16px 16px 0 0;padding:40px 40px 32px;text-align:center'>" +
            "<div style='display:inline-block;background:rgba(255,255,255,0.1);border-radius:12px;padding:10px 20px;margin-bottom:20px'>" +
            "<span style='color:#C4A55A;font-size:22px;font-weight:800;letter-spacing:-0.5px'>Applyra</span>" +
            "</div>" +
            "<h1 style='margin:0 0 8px;color:#FFFFFF;font-size:28px;font-weight:800;line-height:1.2'>Your dream job is waiting.</h1>" +
            "<p style='margin:0;color:#9CA3AF;font-size:16px;line-height:1.5'>Let AI guide you there.</p>" +
            "</td></tr>" +

            // ── Body ─────────────────────────────────────────────────────────
            "<tr><td style='background:#FFFFFF;padding:40px'>" +

            "<p style='margin:0 0 12px;color:#111827;font-size:16px;font-weight:600'>" + greeting + "</p>" +
            collegeNote +
            "<p style='margin:0 0 28px;color:#374151;font-size:15px;line-height:1.6'>Landing the right job in today's market is tough — but with the right tools, you don't have to do it alone. Applyra's AI is built specifically to help students like you stand out, prepare better, and get placed faster.</p>" +

            // Feature cards
            "<table width='100%' cellpadding='0' cellspacing='0'>" +

            featureCard("🤖", "#EEF2FF", "#4F46E5",
                "AI Resume Analyzer",
                "Upload your resume and get an instant ATS compatibility score, plus specific suggestions to make it past automated filters at top companies.") +

            featureCard("🎯", "#ECFDF5", "#059669",
                "Smart Drive Matching",
                "Our AI matches you to placement drives where your profile — CGPA, department, skills — gives you the best shot. No more applying blind.") +

            featureCard("📝", "#FFF7ED", "#D97706",
                "AI Interview Prep",
                "Practice role-specific interview questions generated by AI, get feedback on your answers, and walk into every interview confident and prepared.") +

            featureCard("📊", "#FDF4FF", "#9333EA",
                "Skill Gap Insights",
                "See exactly what skills recruiters in your target companies are looking for — and a personalised roadmap to close the gaps before your next drive.") +

            featureCard("🚀", "#FFF1F2", "#E11D48",
                "One-click Applications",
                "Track every application, offer, and round result in one place. Never miss a deadline or lose track of where you stand.") +

            "</table>" +

            // Social proof
            "<div style='margin:32px 0;padding:20px 24px;background:#F9FAFB;border-left:4px solid #C4A55A;border-radius:0 8px 8px 0'>" +
            "<p style='margin:0;color:#374151;font-size:14px;line-height:1.6;font-style:italic'>\"Applyra's resume analyzer helped me raise my ATS score from 42 to 87. I got shortlisted by 3 companies in the same week.\"</p>" +
            "<p style='margin:8px 0 0;color:#6B7280;font-size:13px;font-weight:600'>— Priya S., placed at Infosys</p>" +
            "</div>" +

            // CTA
            "<div style='text-align:center;margin:32px 0 8px'>" +
            "<a href='https://applyra.in' style='display:inline-block;background:linear-gradient(135deg,#C4A55A,#D4B86A);color:#1A1A2E;font-size:16px;font-weight:800;text-decoration:none;padding:16px 40px;border-radius:50px;letter-spacing:0.3px'>Get started — it's free →</a>" +
            "</div>" +
            "<p style='margin:12px 0 0;text-align:center;color:#9CA3AF;font-size:13px'>No credit card needed · Free plan available</p>" +

            "</td></tr>" +

            // ── Footer ───────────────────────────────────────────────────────
            "<tr><td style='background:#F9FAFB;border-radius:0 0 16px 16px;padding:24px 40px;text-align:center;border-top:1px solid #E5E7EB'>" +
            "<p style='margin:0 0 6px;color:#6B7280;font-size:13px'>You're receiving this because your college is partnered with Applyra.</p>" +
            "<p style='margin:0;color:#9CA3AF;font-size:12px'>Applyra · India's AI-first campus placement platform</p>" +
            "</td></tr>" +

            "</table>" + // inner 600px table
            "</td></tr></table>" + // outer wrapper
            "</body></html>";
    }

    private String featureCard(String emoji, String bgColor, String accentColor, String title, String desc) {
        return "<tr><td style='padding-bottom:16px'>" +
            "<table width='100%' cellpadding='0' cellspacing='0' style='background:" + bgColor + ";border-radius:12px;overflow:hidden'>" +
            "<tr>" +
            "<td width='56' style='padding:20px 0 20px 20px;vertical-align:top'>" +
            "<div style='width:40px;height:40px;background:rgba(255,255,255,0.7);border-radius:10px;display:flex;align-items:center;justify-content:center;font-size:20px;text-align:center;line-height:40px'>" + emoji + "</div>" +
            "</td>" +
            "<td style='padding:20px 20px 20px 14px;vertical-align:top'>" +
            "<div style='color:" + accentColor + ";font-size:15px;font-weight:700;margin-bottom:4px'>" + title + "</div>" +
            "<div style='color:#374151;font-size:13px;line-height:1.55'>" + desc + "</div>" +
            "</td>" +
            "</tr></table>" +
            "</td></tr>";
    }
}

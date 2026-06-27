package college.student;

import helpers.annotations.CollegeRole;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.college.CollegeAccessMiddleware;
import models.body.CollegeLoginRequest;
import models.enums.UserType;
import models.repos.UserRepository;
import models.services.EmailService;
import models.sql.User;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Called once by the frontend after all bulk-upload rows are processed.
 * Sends a summary email to the TPO and all SUPER_ADMIN users.
 *
 * POST /college/students/onboarding-complete
 * Body: {
 *   "total": 50, "created": 45, "skipped": 3, "failed": 2,
 *   "students": [
 *     { "name": "Jane", "email": "jane@…", "enrollmentNumber": "EN001",
 *       "department": "CSE", "passingYear": "2026" },
 *     ...
 *   ]
 * }
 */
@CollegeRole
public enum OnboardingCompleteController implements BaseController {

    INSTANCE;

    @Override
    public void handle(RoutingContext event) {
        CollegeAccessMiddleware.INSTANCE.with(event, new ArrayList<>(), this.getClass())
                .map(this::map)
                .subscribe(
                        o -> ResponseUtils.INSTANCE.writeJsonResponse(event, o),
                        error -> ResponseUtils.INSTANCE.handleError(event, error)
                );
    }

    private Object map(CollegeLoginRequest request) {
        io.vertx.core.json.JsonObject body = request.getRoutingContext().getBodyAsJson();

        int total   = body != null ? body.getInteger("total",   0) : 0;
        int created = body != null ? body.getInteger("created", 0) : 0;
        int skipped = body != null ? body.getInteger("skipped", 0) : 0;
        int failed  = body != null ? body.getInteger("failed",  0) : 0;

        // Build student rows list from body
        List<String[]> studentRows = new ArrayList<>();
        if (body != null && body.containsKey("students")) {
            List<?> rawList = body.getJsonArray("students").getList();
            for (Object obj : rawList) {
                if (obj instanceof Map) {
                    Map<?, ?> m = (Map<?, ?>) obj;
                    studentRows.add(new String[]{
                        str(m.get("name")),
                        str(m.get("email")),
                        str(m.get("enrollmentNumber")),
                        str(m.get("department")),
                        str(m.get("passingYear"))
                    });
                }
            }
        }

        String collegeName = request.getCollege().getName();
        String collegeCode = request.getCollege().getCode() != null ? request.getCollege().getCode() : "";
        String tpoName  = request.getUser() != null && request.getUser().getName()  != null ? request.getUser().getName()  : "TPO";
        String tpoEmail = request.getUser() != null && request.getUser().getEmail() != null ? request.getUser().getEmail() : null;

        final int fTotal   = total;
        final int fCreated = created;
        final int fSkipped = skipped;
        final int fFailed  = failed;
        final List<String[]> fRows = new ArrayList<>(studentRows);
        final String fCollegeName = collegeName;
        final String fCollegeCode = collegeCode;
        final String fTpoName  = tpoName;
        final String fTpoEmail = tpoEmail;
        final String timestamp = ZonedDateTime.now(ZoneId.of("Asia/Kolkata"))
                .format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a z"));

        new Thread(() -> {
            try {
                String summaryHtml = EmailService.buildOnboardingSummaryHtml(
                        fCollegeName, fCollegeCode,
                        fTpoName, fTpoEmail != null ? fTpoEmail : "",
                        fTotal, fCreated, fSkipped, fFailed,
                        fRows, timestamp
                );
                String subject = "[Applyra] " + fCreated + " students onboarded by " + fCollegeName + " (" + fCollegeCode + ")";

                // Send to TPO
                if (fTpoEmail != null && !fTpoEmail.isBlank()) {
                    EmailService.sendEmail(fTpoEmail, subject, summaryHtml)
                            .subscribe(
                                    sent -> System.out.println("[OnboardingComplete] TPO summary " + (sent ? "sent" : "failed") + " to " + fTpoEmail),
                                    err  -> System.err.println("[OnboardingComplete] TPO summary email error: " + err.getMessage())
                            );
                }

                // Send to all super admins
                List<User> superAdmins = UserRepository.INSTANCE.findByUserType(UserType.SUPER_ADMIN);
                for (User admin : superAdmins) {
                    if (admin.getEmail().equals(fTpoEmail)) continue; // avoid duplicate if TPO is also admin
                    EmailService.sendEmail(admin.getEmail(), subject, summaryHtml)
                            .subscribe(
                                    sent -> System.out.println("[OnboardingComplete] Admin summary " + (sent ? "sent" : "failed") + " to " + admin.getEmail()),
                                    err  -> System.err.println("[OnboardingComplete] Admin summary email error: " + err.getMessage())
                            );
                }
            } catch (Exception e) {
                System.err.println("[OnboardingComplete] Summary thread error: " + e.getMessage());
            }
        }, "onboarding-complete-summary").start();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "Summary emails queued");
        return response;
    }

    private String str(Object o) {
        return o != null ? o.toString() : null;
    }
}

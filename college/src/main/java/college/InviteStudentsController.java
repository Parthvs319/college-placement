package college;

import helpers.annotations.CollegeRole;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.college.CollegeAccessMiddleware;
import models.body.CollegeLoginRequest;
import models.enums.UserType;
import models.repos.InviteTokenRepository;
import models.repos.UserRepository;
import models.services.EmailService;
import models.sql.InviteToken;
import models.sql.User;

import java.sql.Timestamp;
import java.util.*;


@CollegeRole
public enum InviteStudentsController implements BaseController {

    INSTANCE;

    private static final long INVITE_EXPIRY_MS = 7L * 24 * 60 * 60 * 1000; // 7 days
    private static final String FRONTEND_URL = System.getenv().getOrDefault("FRONTEND_URL", "http://localhost:5173");

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
        Object emailsObj = request.getRequest().get("emails");
        if (emailsObj == null || !(emailsObj instanceof List)) {
            throw new RoutingError("emails array is required");
        }

        List<String> emails = new ArrayList<>();
        for (Object o : (List<?>) emailsObj) {
            String email = o.toString().trim().toLowerCase();
            if (!email.isEmpty()) emails.add(email);
        }

        if (emails.isEmpty()) {
            throw new RoutingError("At least one email is required");
        }

        if (emails.size() > 100) {
            throw new RoutingError("Maximum 100 invites at a time");
        }

        Long collegeId = request.getCollege().getId();
        String collegeName = request.getCollege().getName();
        User invitedBy = request.getUser();

        List<Map<String, Object>> results = new ArrayList<>();
        int sent = 0;
        int skipped = 0;

        for (String email : emails) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("email", email);

            User existingUser = UserRepository.INSTANCE.byEmail(email);
            if (existingUser != null && existingUser.getUserType() == UserType.STUDENT) {
                item.put("status", "skipped");
                item.put("reason", "Already registered");
                results.add(item);
                skipped++;
                continue;
            }

            InviteToken existingToken = InviteTokenRepository.INSTANCE.byEmailAndCollege(email, collegeId);
            if (existingToken != null && !existingToken.used
                    && existingToken.expiresAt.after(new Timestamp(System.currentTimeMillis()))) {
                // Resend the existing invite
                sendInviteEmail(email, existingToken.token, collegeName);
                item.put("status", "resent");
                item.put("reason", "Invite already existed, resent email");
                results.add(item);
                sent++;
                continue;
            }

            InviteToken token = new InviteToken();
            token.token = UUID.randomUUID().toString();
            token.email = email;
            token.college = request.getCollege();
            token.userType = UserType.STUDENT;
            token.expiresAt = new Timestamp(System.currentTimeMillis() + INVITE_EXPIRY_MS);
            token.invitedBy = invitedBy;
            token.save();

            sendInviteEmail(email, token.token, collegeName);

            item.put("status", "sent");
            results.add(item);
            sent++;
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "Invites processed");
        response.put("total", emails.size());
        response.put("sent", sent);
        response.put("skipped", skipped);
        response.put("results", results);
        return response;
    }

    private void sendInviteEmail(String email, String token, String collegeName) {
        String registerUrl = FRONTEND_URL + "/register?token=" + token;
        String html = EmailService.buildStudentInviteHtml(collegeName, registerUrl);
        new Thread(() -> {
            try {
                EmailService.sendEmail(email, "You're invited to join " + collegeName + " on Applyra", html)
                        .subscribe(
                                sent -> System.out.println("[InviteStudents] Email " + (sent ? "sent" : "failed") + " to " + email),
                                err -> System.err.println("[InviteStudents] Email error for " + email + ": " + err.getMessage())
                        );
            } catch (Exception e) {
                System.err.println("[InviteStudents] Email thread error for " + email + ": " + e.getMessage());
            }
        }, "invite-email-" + email).start();
    }
}

package superadmin.college;

import helpers.annotations.SuperAdminRole;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.PasswordUtils;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.superadmin.SuperAdminAccessMiddleware;
import models.body.SuperAdminLoginRequest;
import models.enums.UserType;
import models.repos.CollegeRepository;
import models.repos.UserRepository;
import models.services.EmailService;
import models.sql.College;
import models.sql.User;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * POST /admin/colleges/:collegeId/tpo
 *
 * Body (JSON):
 *   { "tpoEmail": "tpo@college.edu.in", "tpoName": "Dr. Rajesh Sharma" }
 *
 * Creates a TPO user for the college (or links an existing one).
 * Sends onboarding email with auto-generated credentials if a new account is created.
 */
@SuperAdminRole(request = {
        "tpoEmail:string@required",
        "tpoName:string"
})
public enum AddTpoController implements BaseController {

    INSTANCE;

    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
    private static final SecureRandom RNG = new SecureRandom();

    @Override
    public void handle(RoutingContext event) {
        SuperAdminAccessMiddleware.INSTANCE.with(event, new ArrayList<>(), this.getClass())
                .map(req -> map(req, event))
                .subscribe(
                        o -> ResponseUtils.INSTANCE.writeJsonResponse(event, o),
                        error -> ResponseUtils.INSTANCE.handleError(event, error)
                );
    }

    private Object map(SuperAdminLoginRequest request, RoutingContext rc) {
        Long collegeId = Long.parseLong(rc.pathParam("collegeId"));
        College college = CollegeRepository.INSTANCE.byId(collegeId);
        if (college == null) throw new RoutingError(404, "College not found");

        var body = request.getRequest();
        String tpoEmail = ((String) body.get("tpoEmail")).trim().toLowerCase();
        String tpoName  = body.get("tpoName");
        if (tpoName != null && tpoName.isBlank()) tpoName = null;

        User tpoUser;
        String generatedPassword = null;

        User existing = UserRepository.INSTANCE.byEmail(tpoEmail);
        if (existing != null) {
            // Already exists — must belong to this college
            if (existing.getCollege() == null || !existing.getCollege().getId().equals(collegeId)) {
                throw new RoutingError("This email is already registered to a different college.");
            }
            tpoUser = existing;
        } else {
            generatedPassword = generatePassword();
            tpoUser = new User();
            tpoUser.setEmail(tpoEmail);
            tpoUser.setName(tpoName != null ? tpoName : "TPO");
            tpoUser.setPassword(PasswordUtils.INSTANCE.hash(generatedPassword));
            tpoUser.setUserType(UserType.TPO);
            tpoUser.setCollege(college);
            tpoUser.setVerified(true);
            tpoUser.setActive(true);
            tpoUser.save();
        }

        // Send credentials email in background (only for new accounts)
        if (generatedPassword != null) {
            final String fEmail    = tpoEmail;
            final String fName     = tpoUser.getName();
            final String fPassword = generatedPassword;
            final String fCollege  = college.getName();
            final String portalUrl = System.getenv().getOrDefault("PORTAL_URL", "https://applyra.netlify.app");

            new Thread(() -> {
                try {
                    // Reuse the contract-with-credentials email; pass nulls for contract fields
                    String html = EmailService.buildContractWithCredentialsHtml(
                            fName, fCollege, college.getCode(),
                            fEmail, fPassword,
                            null, null, null, portalUrl,
                            null, null
                    );
                    String subject = "Your Applyra TPO Account | " + fCollege;
                    EmailService.sendEmail(fEmail, subject, html)
                            .subscribe(
                                    sent -> System.out.println("[AddTpo] Email " + (sent ? "sent" : "failed") + " to " + fEmail),
                                    err  -> System.err.println("[AddTpo] Email error: " + err.getMessage())
                            );
                } catch (Exception e) {
                    System.err.println("[AddTpo] Email thread error: " + e.getMessage());
                }
            }, "add-tpo-email").start();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("userId",    tpoUser.getId());
        result.put("tpoEmail",  tpoUser.getEmail());
        result.put("tpoName",   tpoUser.getName());
        result.put("created",   generatedPassword != null);
        result.put("message",   generatedPassword != null
                ? "TPO account created and credentials emailed to " + tpoEmail
                : "Existing TPO account linked to this college");
        return result;
    }

    private static String generatePassword() {
        StringBuilder sb = new StringBuilder(12);
        for (int i = 0; i < 12; i++) sb.append(CHARS.charAt(RNG.nextInt(CHARS.length())));
        return sb.toString();
    }
}

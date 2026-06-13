package superadmin;

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
import java.util.List;
import java.util.Map;

@SuperAdminRole
public enum VerifyCollegeController implements BaseController {

    INSTANCE;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";

    @Override
    public void handle(RoutingContext event) {
        SuperAdminAccessMiddleware.INSTANCE.with(event, new ArrayList<>(), this.getClass())
                .map(this::map)
                .subscribe(
                        o -> ResponseUtils.INSTANCE.writeJsonResponse(event, o),
                        error -> ResponseUtils.INSTANCE.handleError(event, error)
                );
    }

    private Object map(SuperAdminLoginRequest request) {
        Long collegeId = Long.parseLong(request.getRoutingContext().pathParam("collegeId"));
        College college = CollegeRepository.INSTANCE.byId(collegeId);
        if (college == null) throw new RoutingError(404, "College not found");
        if (college.verified) {
            throw new RoutingError("College is already verified");
        }
        college.setActive(true);
        college.setVerified(true);
        college.update();

        List<User> existingTpos = UserRepository.INSTANCE.byCollegeAndType(collegeId, UserType.TPO);
        if (!existingTpos.isEmpty()) {
            return Map.of(
                    "message", "College verified successfully. TPO account already exists.",
                    "collegeId", collegeId,
                    "tpoEmail", existingTpos.get(0).email
            );
        }
        String rawPassword = generatePassword(12);
        String tpoEmail = college.contactEmail;
        if (tpoEmail == null || tpoEmail.isBlank()) {
            return Map.of(
                    "message", "College verified but no contactEmail set — TPO account not created",
                    "collegeId", collegeId
            );
        }
        User existingUser = UserRepository.INSTANCE.byEmail(tpoEmail);
        if (existingUser != null) {
            if (existingUser.college == null) {
                existingUser.college = college;
                existingUser.userType = UserType.TPO;
                existingUser.verified = true;
                existingUser.active = true;
                existingUser.save();
            }
            return Map.of(
                    "message", "College verified. Existing user linked as TPO.",
                    "collegeId", collegeId,
                    "tpoEmail", tpoEmail
            );
        }

        User tpo = new User();
        tpo.email = tpoEmail;
        tpo.password = PasswordUtils.INSTANCE.hash(rawPassword);
        tpo.name = college.name + " TPO";
        tpo.userType = UserType.TPO;
        tpo.college = college;
        tpo.verified = true;
        tpo.active = true;
        tpo.save();
        try {
            String html = EmailService.buildTpoCredentialsHtml(
                    college.name, tpoEmail, rawPassword, college.code
            );
            EmailService.sendEmail(tpoEmail, "Your Applyra TPO Login Credentials — " + college.name, html)
                    .subscribe(
                            sent -> System.out.println("[VerifyCollege] Credentials email " + (sent ? "sent" : "failed") + " to " + tpoEmail),
                            err -> System.err.println("[VerifyCollege] Email error: " + err.getMessage())
                    );
        } catch (Exception e) {
            System.err.println("[VerifyCollege] Email thread error: " + e.getMessage());
        }

        return Map.of(
                "message", "College verified and TPO account created. Credentials sent to " + tpoEmail,
                "collegeId", collegeId,
                "tpoEmail", tpoEmail,
                "tpoUserId", tpo.getId()
        );
    }

    private String generatePassword(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }
}

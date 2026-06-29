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
        if (college.isVerified()) {
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
                    "tpoEmail", existingTpos.get(0).getEmail()
            );
        }
        String rawPassword = generatePassword(12);
        String tpoEmail = college.getContactEmail();
        if (tpoEmail == null || tpoEmail.isBlank()) {
            return Map.of(
                    "message", "College verified but no contactEmail set — TPO account not created",
                    "collegeId", collegeId
            );
        }
        User existingUser = UserRepository.INSTANCE.byEmailAndUserType(tpoEmail , UserType.TPO);
        if (existingUser != null) {
            if (existingUser.getCollege() == null) {
                existingUser.setCollege(college);
                existingUser.setUserType(UserType.TPO);
                existingUser.setVerified(true);
                existingUser.setActive(true);
                existingUser.save();
            }
            return Map.of(
                    "message", "College verified. Existing user linked as TPO.",
                    "collegeId", collegeId,
                    "tpoEmail", tpoEmail
            );
        }

        User tpo = new User();
        tpo.setEmail(tpoEmail);
        tpo.setPassword(PasswordUtils.INSTANCE.hash(rawPassword));
        tpo.setName(college.getName() + " TPO");
        tpo.setUserType(UserType.TPO);
        tpo.setCollege(college);
        tpo.setVerified(true);
        tpo.setActive(true);
        tpo.save();
        try {
            String html = EmailService.buildTpoCredentialsHtml(
                    college.getName(), tpoEmail, rawPassword, college.getCode()
            );
            EmailService.sendEmail(tpoEmail, "Your Applyra TPO Login Credentials | " + college.getName(), html)
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

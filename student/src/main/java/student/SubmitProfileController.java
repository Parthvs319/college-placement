package student;

import helpers.annotations.StudentRole;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.student.StudentAccessMiddleware;
import models.body.StudentLoginRequest;
import models.sql.Student;
import models.sql.User;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Student submits their profile for verification.
 * Auto-verifies the account if minimum required fields are filled.
 *
 * Required before submitting:
 *   - mobile (on User)
 *   - dateOfBirth
 *   - gender
 *   - tenthPercentage
 *
 * POST /student/me/submit-profile
 */
@StudentRole
public enum SubmitProfileController implements BaseController {

    INSTANCE;

    @Override
    public void handle(RoutingContext event) {
        StudentAccessMiddleware.INSTANCE.with(event, new ArrayList<>(), this.getClass())
                .map(this::map)
                .subscribe(
                        o -> ResponseUtils.INSTANCE.writeJsonResponse(event, o),
                        error -> ResponseUtils.INSTANCE.handleError(event, error)
                );
    }

    private Object map(StudentLoginRequest request) {
        Student student = request.getStudent();
        User user = request.getUser();

        if (student == null) throw new RoutingError("Student profile not found");

        if (student.isProfileComplete()) {
            throw new RoutingError("Profile already submitted and verified");
        }

        // Minimum required fields check
        if (user.getMobile() == null || user.getMobile().isBlank()) {
            throw new RoutingError("Mobile number is required before submitting profile");
        }
        if (student.getDateOfBirth() == null || student.getDateOfBirth().isBlank()) {
            throw new RoutingError("Date of birth is required before submitting profile");
        }
        if (student.getGender() == null || student.getGender().isBlank()) {
            throw new RoutingError("Gender is required before submitting profile");
        }
        if (student.getTenthPercentage() == null || student.getTenthPercentage().isBlank()) {
            throw new RoutingError("10th percentage is required before submitting profile");
        }

        // Mark profile complete + auto-verify
        student.profileComplete    = true;
        student.profileSubmittedAt = new Timestamp(System.currentTimeMillis());
        student.update();

        user.verified = true;
        user.update();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "Profile submitted successfully. Your account is now verified.");
        response.put("verified", true);
        response.put("profileComplete", true);
        response.put("profile", StudentDtos.toProfileDto(student));
        return response;
    }
}

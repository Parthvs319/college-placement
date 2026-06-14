package user;

import helpers.blueprint.enums.RequestItemType;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.*;
import io.vertx.rxjava.core.RxHelper;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.tokens.BearerToken;
import models.access.tokens.TokenService;
import models.enums.UserType;
import models.repos.InviteTokenRepository;
import models.repos.StudentRepository;
import models.repos.UserRepository;
import models.sql.InviteToken;
import models.sql.Student;
import models.sql.User;
import rx.Single;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Public endpoint (no auth) — student registers using an invite token from TPO.
 * Token validates college + email, so the student is auto-verified.
 *
 * POST /user/register/invite
 * Body: { "token": "uuid", "name": "John Doe", "password": "secret123",
 *         "enrollmentNumber": "0901CS201001", "department": "CSE",
 *         "passingYear": 2025, "mobile": "9876543210",
 *         "cgpa": "8.5", "gender": "Male" }
 */
public enum StudentRegisterByInviteController implements BaseController {

    INSTANCE;

    public List<RequestItem> items() {
        List<RequestItem> items = new ArrayList<>();
        items.add(RequestItem.builder().key("token").itemType(RequestItemType.STRING).required(true).build());
        items.add(RequestItem.builder().key("name").itemType(RequestItemType.STRING).required(true).build());
        items.add(RequestItem.builder().key("password").itemType(RequestItemType.STRING).required(true).build());
        items.add(RequestItem.builder().key("enrollmentNumber").itemType(RequestItemType.STRING).required(true).build());
        items.add(RequestItem.builder().key("department").itemType(RequestItemType.STRING).required(false).build());
        items.add(RequestItem.builder().key("passingYear").itemType(RequestItemType.STRING).required(false).build());
        items.add(RequestItem.builder().key("mobile").itemType(RequestItemType.STRING).required(false).build());
        items.add(RequestItem.builder().key("cgpa").itemType(RequestItemType.STRING).required(false).build());
        items.add(RequestItem.builder().key("gender").itemType(RequestItemType.STRING).required(false).build());
        return items;
    }

    private RequestZipped parseRequest(RoutingContext event) {
        return RequestHelper.INSTANCE.requestZipped(event, items());
    }

    @Override
    public void handle(RoutingContext event) {
        Single.just(event)
                .subscribeOn(RxHelper.blockingScheduler(event.vertx()))
                .map(this::parseRequest)
                .map(this::register)
                .subscribe(
                        o -> ResponseUtils.INSTANCE.writeJsonResponse(event, o),
                        error -> ResponseUtils.INSTANCE.handleError(event, error)
                );
    }

    private Object register(RequestZipped request) {
        String tokenStr = request.getRequest().get("token");
        String name = request.getRequest().get("name");
        String password = request.getRequest().get("password");
        String enrollmentNumber = request.getRequest().get("enrollmentNumber");
        String department = request.getRequest().get("department");
        String passingYearStr = request.getRequest().get("passingYear");
        String mobile = request.getRequest().get("mobile");
        String cgpaStr = request.getRequest().get("cgpa");
        String gender = request.getRequest().get("gender");

        // Validate token
        InviteToken inviteToken = InviteTokenRepository.INSTANCE.findValidToken(tokenStr);
        if (inviteToken == null) {
            throw new RoutingError("Invalid or expired invite token");
        }

        String email = inviteToken.email;

        // Check if user already exists
        User existingUser = UserRepository.INSTANCE.byEmail(email);
        if (existingUser != null) {
            throw new RoutingError("An account with this email already exists. Please login instead.");
        }

        // Check enrollment number uniqueness at this college
        Long collegeId = inviteToken.college.getId();
        Student existingStudent = StudentRepository.INSTANCE.byEnrollment(enrollmentNumber.trim(), collegeId);
        if (existingStudent != null) {
            throw new RoutingError("Enrollment number already registered at this college");
        }

        // Validate password
        if (password.length() < 6) {
            throw new RoutingError("Password must be at least 6 characters");
        }

        // Validate mobile if provided
        if (mobile != null && !mobile.trim().isEmpty()) {
            mobile = mobile.trim();
            if (!mobile.matches("^[6-9][0-9]{9}$")) {
                throw new RoutingError("Enter a valid 10-digit Indian mobile number");
            }
        }

        // Create User
        User user = new User();
        user.email = email;
        user.name = name.trim();
        user.password = PasswordUtils.INSTANCE.hash(password);
        user.mobile = mobile;
        user.userType = UserType.STUDENT;
        user.college = inviteToken.college;
        user.verified = true;   // invite-based = pre-verified
        user.active = true;
        user.save();

        // Create Student
        Student student = new Student();
        student.user = user;
        student.college = inviteToken.college;
        student.enrollmentNumber = enrollmentNumber.trim();
        student.department = department != null ? department.trim() : null;
        student.gender = gender != null ? gender.trim() : null;

        if (passingYearStr != null && !passingYearStr.isBlank()) {
            try {
                student.passingYear = Integer.parseInt(passingYearStr.trim());
            } catch (NumberFormatException ignored) {}
        }

        if (cgpaStr != null && !cgpaStr.isBlank()) {
            try {
                student.cgpa = new BigDecimal(cgpaStr.trim());
            } catch (NumberFormatException ignored) {}
        }

        student.save();

        // Mark invite token as used
        inviteToken.used = true;
        inviteToken.update();

        // Generate JWT for the new user (auto-login after registration)
        BearerToken bearerToken = TokenService.generateToken(
                user.getId(), user.getEmail(), user.getUserType().getValue(), user.getName()
        );

        // Return response with token so frontend can auto-login
        return new RegisterResponse(true, bearerToken, user.getId(), student.getId(), email, inviteToken.college.getName());
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    static class RegisterResponse {
        boolean success;
        BearerToken bearerToken;
        Long userId;
        Long studentId;
        String email;
        String collegeName;
    }
}

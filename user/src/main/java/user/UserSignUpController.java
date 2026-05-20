package user;

import helpers.annotations.UserAnnotation;
import helpers.blueprint.enums.RequestItemType;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.*;
import io.vertx.rxjava.core.RxHelper;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.repos.CollegeRepository;
import models.repos.UserRepository;
import models.sql.College;
import models.sql.User;
import models.enums.UserType;
import models.access.tokens.BearerToken;
import models.access.tokens.TokenService;
import rx.Single;

import java.util.ArrayList;
import java.util.List;

@UserAnnotation
public enum UserSignUpController implements BaseController {

    INSTANCE;

    public List<RequestItem> items() {
        List<RequestItem> items = new ArrayList<>();
        items.add(RequestItem.builder().key("email").itemType(RequestItemType.STRING).required(true).build());
        items.add(RequestItem.builder().key("password").itemType(RequestItemType.STRING).required(true).build());
        items.add(RequestItem.builder().key("name").itemType(RequestItemType.STRING).required(true).build());
        items.add(RequestItem.builder().key("mobile").itemType(RequestItemType.STRING).required(false).build());
        items.add(RequestItem.builder().key("userType").itemType(RequestItemType.STRING).required(true).build());
        items.add(RequestItem.builder().key("collegeCode").itemType(RequestItemType.STRING).required(false).build());
        return items;
    }

    private RequestZipped map(RoutingContext event) {
        return RequestHelper.INSTANCE.requestZipped(event, items());
    }

    @Override
    public void handle(RoutingContext event) {
        Single.just(event)
                .subscribeOn(RxHelper.blockingScheduler(event.vertx()))
                .map(this::map)
                .map(this::map)
                .subscribe(
                        o -> ResponseUtils.INSTANCE.writeJsonResponse(event, o),
                        error -> ResponseUtils.INSTANCE.handleError(event, error)
                );
    }

    private SuccessResponse map(RequestZipped request) {
        String email = request.getRequest().get("email");
        String password = request.getRequest().get("password");
        String name = request.getRequest().get("name");
        String mobile = request.getRequest().get("mobile");
        String userType = request.getRequest().get("userType");
        String collegeCode = request.getRequest().get("collegeCode");

        if (email == null || email.trim().isEmpty()) {
            throw new RoutingError("Email is required");
        }
        if (password == null || password.isEmpty()) {
            throw new RoutingError("Password is required");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new RoutingError("Name is required");
        }

        // Validate mobile if provided
        if (mobile != null && !mobile.trim().isEmpty()) {
            mobile = mobile.trim();
            if (!mobile.matches("^[6-9][0-9]{9}$")) {
                throw new RoutingError("Enter a valid 10-digit Indian mobile number");
            }
            if (UserRepository.INSTANCE.byMobile(mobile) != null) {
                throw new RoutingError("User with this mobile already exists");
            }
        }

        if (UserRepository.INSTANCE.byEmail(email.trim()) != null) {
            throw new RoutingError("User with this email already exists");
        }

        UserType type = UserType.valueOf(userType.toUpperCase());

        // ── College verification ──────────────────────────────
        // STUDENT, COLLEGE_ADMIN, and TPO must provide a valid collegeCode
        College college = null;
        if (type == UserType.STUDENT || type == UserType.COLLEGE_ADMIN || type == UserType.TPO) {
            if (collegeCode == null || collegeCode.trim().isEmpty()) {
                throw new RoutingError("collegeCode is required for " + type.name() + " users");
            }
            college = CollegeRepository.INSTANCE.byCode(collegeCode.trim().toUpperCase());
            if (college == null) {
                throw new RoutingError("No college found with code: " + collegeCode);
            }
            if (!college.active) {
                throw new RoutingError("This college is not currently active on the platform");
            }

            // Auto-verify if email domain matches the college's contact email domain
            // e.g. student email: john@sgsits.ac.in, college email: tpo@sgsits.ac.in → match
        }

        User user = new User();
        user.email = email.trim();
        user.password = PasswordUtils.INSTANCE.hash(password);
        user.name = name.trim();
        user.mobile = mobile;
        user.userType = type;
        user.college = college;

        // Auto-verify based on email domain match
        if (college != null && college.contactEmail != null) {
            String collegeDomain = college.contactEmail.substring(college.contactEmail.indexOf("@") + 1);
            String userDomain = email.trim().substring(email.trim().indexOf("@") + 1);
            if (collegeDomain.equalsIgnoreCase(userDomain)) {
                user.verified = true;
            }
        }

        user.save();

        BearerToken token = TokenService.generateToken(
                user.getId(),
                user.getEmail(),
                user.getUserType().name(),
                user.getName()
        );
        return new SuccessResponse(true, token.toString());
    }
}

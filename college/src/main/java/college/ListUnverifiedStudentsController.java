package college;

import helpers.annotations.UserAnnotation;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.user.UserAccessMiddleware;
import models.body.UserLoginRequest;
import models.enums.UserType;
import models.repos.StudentRepository;
import models.sql.Student;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * TPO / College Admin lists students whose user.verified = false.
 */
@UserAnnotation
public enum ListUnverifiedStudentsController implements BaseController {

    INSTANCE;

    @Override
    public void handle(RoutingContext event) {
        UserAccessMiddleware.INSTANCE.with(event, new ArrayList<>(), this.getClass())
                .map(this::map)
                .subscribe(
                        o -> ResponseUtils.INSTANCE.writeJsonResponse(event, o),
                        error -> ResponseUtils.INSTANCE.handleError(event, error)
                );
    }

    private Object map(UserLoginRequest request) {
        UserType userType = request.getUser().getUserType();
        if (!userType.equals(UserType.COLLEGE_ADMIN) && !userType.equals(UserType.TPO)) {
            throw new RoutingError("Not authorized");
        }

        Long collegeId = request.getUser().college.getId();

        List<Student> allStudents = StudentRepository.INSTANCE.byCollege(collegeId);
        List<Student> unverified = allStudents.stream()
                .filter(s -> !s.user.verified)
                .collect(Collectors.toList());

        return unverified;
    }
}

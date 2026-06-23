package college;

import helpers.annotations.CollegeRole;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.college.CollegeAccessMiddleware;
import models.body.CollegeLoginRequest;
import models.repos.StudentRepository;
import models.sql.Student;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * TPO / College Admin lists students whose user.verified = false.
 */
@CollegeRole
public enum ListUnverifiedStudentsController implements BaseController {

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
        Long collegeId = request.getCollege().getId();

        List<Student> allStudents = StudentRepository.INSTANCE.byCollege(collegeId);
        return allStudents.stream()
                .filter(s -> s.getUser() != null && !s.getUser().isVerified())
                .map(ListStudentsController::toDto)
                .collect(Collectors.toList());
    }
}

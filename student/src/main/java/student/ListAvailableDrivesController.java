package student;

import helpers.annotations.UserAnnotation;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.user.UserAccessMiddleware;
import models.body.UserLoginRequest;
import models.repos.DriveRepository;
import models.repos.StudentRepository;
import models.sql.Student;

import java.util.ArrayList;
import java.util.stream.Collectors;


@UserAnnotation
public enum ListAvailableDrivesController implements BaseController {

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
        Student student = StudentRepository.INSTANCE.byUserId(request.getUser().getId());
        if (student == null) {
            throw new RoutingError("Student profile not found. Complete onboarding first.");
        }
        Long collegeId = student.college.getId();
        return DriveRepository.INSTANCE.upcoming(collegeId).stream()
                .map(StudentDtos::toDriveListDto).collect(Collectors.toList());
    }
}

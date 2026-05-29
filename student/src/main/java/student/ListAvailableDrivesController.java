package student;

import helpers.annotations.StudentRole;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.student.StudentAccessMiddleware;
import models.body.StudentLoginRequest;
import models.repos.DriveRepository;
import models.repos.StudentRepository;
import models.sql.Student;

import java.util.ArrayList;
import java.util.stream.Collectors;


@StudentRole
public enum ListAvailableDrivesController implements BaseController {

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
        Student student = StudentRepository.INSTANCE.byUserId(request.getUser().getId());
        if (student == null) {
            throw new RoutingError("Student profile not found. Complete onboarding first.");
        }
        Long collegeId = student.college.getId();
        return DriveRepository.INSTANCE.upcoming(collegeId).stream()
                .map(StudentDtos::toDriveListDto).collect(Collectors.toList());
    }
}

package student;

import helpers.annotations.StudentRole;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.Request;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.student.StudentAccessMiddleware;
import models.body.StudentLoginRequest;
import models.repos.StudentRepository;
import models.sql.Student;

import java.math.BigDecimal;
import java.util.ArrayList;

@StudentRole
public enum UpdateProfileController implements BaseController {

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
            throw new RoutingError("Student profile not found");
        }

        Request body = request.getRequest();

        if (body.isPresent("cgpa")) student.cgpa = new BigDecimal(String.valueOf(body.get("cgpa")));
        if (body.isPresent("activeBacklogs")) student.activeBacklogs = Integer.parseInt(body.get("activeBacklogs"));
        if (body.isPresent("totalBacklogs")) student.totalBacklogs = Integer.parseInt(body.get("totalBacklogs"));
        if (body.isPresent("tenthPercentage")) student.tenthPercentage = body.get("tenthPercentage");
        if (body.isPresent("twelfthPercentage")) student.twelfthPercentage = body.get("twelfthPercentage");
        if (body.isPresent("diplomaPercentage")) student.diplomaPercentage = body.get("diplomaPercentage");
        if (body.isPresent("linkedinUrl")) student.linkedinUrl = body.get("linkedinUrl");
        if (body.isPresent("githubUrl")) student.githubUrl = body.get("githubUrl");
        if (body.isPresent("portfolioUrl")) student.portfolioUrl = body.get("portfolioUrl");
        if (body.isPresent("resumeUrl")) student.resumeUrl = body.get("resumeUrl");

        student.update();
        return StudentDtos.toProfileDto(student);
    }
}

package student;

import helpers.annotations.CollegeRole;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import lombok.Data;
import models.access.middlewear.college.CollegeAccessMiddleware;
import models.body.CollegeLoginRequest;
import models.repos.StudentRepository;
import models.repos.UserRepository;
import models.sql.Student;
import models.sql.User;

import java.util.ArrayList;

@CollegeRole(tpoAllowed = true)
public enum VerifyStudentController implements BaseController {

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
        Long myCollegeId = request.getCollege().getId();

        String studentIdParam = request.getRoutingContext().pathParam("studentId");
        Long studentId = Long.parseLong(studentIdParam);

        Student student = StudentRepository.INSTANCE.byId(studentId);
        if (student == null) {
            throw new RoutingError("Student not found");
        }

        if (!student.college.getId().equals(myCollegeId)) {
            throw new RoutingError("You can only verify students from your own college");
        }

        User studentUser = student.user;
        if (studentUser.verified) {
            throw new RoutingError("Student is already verified");
        }

        studentUser.verified = true;
        studentUser.update();

        VerifyResponse response = new VerifyResponse();
        response.studentId = student.getId();
        response.enrollmentNumber = student.enrollmentNumber;
        response.name = studentUser.name;
        response.verified = true;
        response.message = "Student verified successfully";
        return response;
    }

    @Data
    static class VerifyResponse {
        Long studentId;
        String enrollmentNumber;
        String name;
        boolean verified;
        String message;
    }
}

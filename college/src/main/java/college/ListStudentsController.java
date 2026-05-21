package college;

import helpers.annotations.UserAnnotation;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import lombok.Data;
import models.access.middlewear.user.UserAccessMiddleware;
import models.body.UserLoginRequest;
import models.enums.UserType;
import models.repos.StudentRepository;
import models.sql.Student;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@UserAnnotation
public enum ListStudentsController implements BaseController {

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
        if (!userType.equals(UserType.COLLEGE_ADMIN) && !userType.equals(UserType.TPO) && !userType.equals(UserType.SUPER_ADMIN)) {
            throw new RoutingError("Not authorized to view student list");
        }

        Long collegeId = request.getUser().college.getId();
        List<Student> students = StudentRepository.INSTANCE.byCollege(collegeId);
        return students.stream().map(ListStudentsController::toDto).collect(Collectors.toList());
    }

    static StudentListItem toDto(Student s) {
        StudentListItem dto = new StudentListItem();
        dto.id = s.getId();
        dto.name = s.user != null ? s.user.name : null;
        dto.email = s.user != null ? s.user.email : null;
        dto.enrollmentNumber = s.enrollmentNumber;
        dto.department = s.department;
        dto.passingYear = s.passingYear;
        dto.cgpa = s.cgpa;
        dto.activeBacklogs = s.activeBacklogs;
        dto.placed = s.placed;
        dto.verified = s.user != null && s.user.verified;
        dto.optedOut = s.optedOut;
        dto.currentCtc = s.currentCtc;
        return dto;
    }

    @Data
    static class StudentListItem {
        Long id;
        String name;
        String email;
        String enrollmentNumber;
        String department;
        int passingYear;
        BigDecimal cgpa;
        int activeBacklogs;
        boolean placed;
        boolean verified;
        boolean optedOut;
        BigDecimal currentCtc;
    }
}

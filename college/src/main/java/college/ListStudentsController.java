package college;

import helpers.annotations.CollegeRole;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import lombok.Data;
import models.access.middlewear.college.CollegeAccessMiddleware;
import models.body.CollegeLoginRequest;
import models.repos.StudentRepository;
import models.sql.Student;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@CollegeRole
public enum ListStudentsController implements BaseController {

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

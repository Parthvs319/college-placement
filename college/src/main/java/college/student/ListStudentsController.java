package college.student;

import helpers.annotations.CollegeRole;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import lombok.Data;
import models.access.middlewear.college.CollegeAccessMiddleware;
import models.body.CollegeLoginRequest;
import models.repos.StudentRepository;
import models.sql.Student;
import models.sql.User;

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
        User user = s.getUser();
        dto.name               = user != null ? user.getName()     : null;
        dto.email              = user != null ? user.getEmail()    : null;
        dto.mobile             = user != null ? user.getMobile()   : null;
        dto.enrollmentNumber   = s.getEnrollmentNumber();
        dto.department         = s.getDepartment();
        dto.passingYear        = s.getPassingYear();
        dto.cgpa               = s.getCgpa();
        dto.activeBacklogs     = s.getActiveBacklogs();
        dto.totalBacklogs      = s.getTotalBacklogs();
        dto.placed             = s.isPlaced();
        dto.verified           = user != null && user.isVerified();
        dto.optedOut           = s.isOptedOut();
        dto.profileComplete    = s.isProfileComplete();
        dto.currentCtc         = s.getCurrentCtc();
        dto.gender             = s.getGender();
        dto.category           = s.getCategory();
        dto.dateOfBirth        = s.getDateOfBirth();
        dto.tenthPercentage    = s.getTenthPercentage();
        dto.twelfthPercentage  = s.getTwelfthPercentage();
        dto.aadharNumber       = s.getAadharNumber();
        dto.panNumber          = s.getPanNumber();
        dto.linkedinUrl        = s.getLinkedinUrl();
        dto.githubUrl          = s.getGithubUrl();
        dto.studentCollegeId   = s.getStudentCollegeId();
        return dto;
    }

    @Data
    static class StudentListItem {
        Long id;
        String name;
        String email;
        String mobile;
        String enrollmentNumber;
        String department;
        int passingYear;
        BigDecimal cgpa;
        int activeBacklogs;
        int totalBacklogs;
        boolean placed;
        boolean verified;
        boolean optedOut;
        boolean profileComplete;
        BigDecimal currentCtc;
        String gender;
        String category;
        String dateOfBirth;
        String tenthPercentage;
        String twelfthPercentage;
        String aadharNumber;
        String panNumber;
        String linkedinUrl;
        String githubUrl;
        String studentCollegeId;
    }
}

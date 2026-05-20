package student;

import helpers.annotations.UserAnnotation;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.Request;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.user.UserAccessMiddleware;
import models.body.UserLoginRequest;
import models.enums.UserType;
import models.repos.CollegeRepository;
import models.repos.StudentRepository;
import models.sql.College;
import models.sql.Student;
import models.sql.User;

import java.math.BigDecimal;
import java.util.ArrayList;

@UserAnnotation
public enum StudentOnboardController implements BaseController {

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
        User user = request.getUser();

        if (!user.getUserType().equals(UserType.STUDENT)) {
            throw new RoutingError("Only STUDENT users can create a student profile");
        }

        if (user.college == null) {
            throw new RoutingError("Your account is not linked to any college. Please contact support.");
        }

        Student existing = StudentRepository.INSTANCE.byUserId(user.getId());
        if (existing != null) {
            throw new RoutingError("Student profile already exists. Use PUT /student/me to update.");
        }

        College college = CollegeRepository.INSTANCE.byId(user.college.getId());
        if (college == null || !college.active) {
            throw new RoutingError("Your college is not active on the platform");
        }

        Request body = request.getRequest();

        String enrollmentNumber = body.get("enrollmentNumber");
        if (enrollmentNumber == null || enrollmentNumber.toString().trim().isEmpty()) {
            throw new RoutingError("enrollmentNumber is required");
        }
        enrollmentNumber = enrollmentNumber.toString().trim();

        Student duplicate = StudentRepository.INSTANCE.byEnrollment(enrollmentNumber, college.getId());
        if (duplicate != null) {
            throw new RoutingError("A student with enrollment number " + enrollmentNumber + " is already registered at this college");
        }

        String department = body.get("department");
        if (department != null && college.departments != null && !college.departments.isEmpty()) {
            if (!college.departments.contains(department.toString().trim())) {
                throw new RoutingError("Department '" + department + "' is not valid for this college. Valid: " + college.departments);
            }
        }

        Student student = new Student();
        student.user = user;
        student.college = college;
        student.enrollmentNumber = enrollmentNumber;

        if (body.isPresent("department")) student.department = body.get("department");
        if (body.isPresent("passingYear")) student.passingYear = Integer.parseInt(String.valueOf(body.get("passingYear")));
        if (body.isPresent("cgpa")) student.cgpa = new BigDecimal(String.valueOf(body.get("cgpa")));
        if (body.isPresent("activeBacklogs")) student.activeBacklogs = Integer.parseInt(String.valueOf(body.get("activeBacklogs")));
        if (body.isPresent("totalBacklogs")) student.totalBacklogs = Integer.parseInt(String.valueOf(body.get("totalBacklogs")));
        if (body.isPresent("tenthPercentage")) student.tenthPercentage = body.get("tenthPercentage");
        if (body.isPresent("twelfthPercentage")) student.twelfthPercentage = body.get("twelfthPercentage");
        if (body.isPresent("diplomaPercentage")) student.diplomaPercentage = body.get("diplomaPercentage");
        if (body.isPresent("gender")) student.gender = body.get("gender");
        if (body.isPresent("dateOfBirth")) student.dateOfBirth = body.get("dateOfBirth");
        if (body.isPresent("linkedinUrl")) student.linkedinUrl = body.get("linkedinUrl");
        if (body.isPresent("githubUrl")) student.githubUrl = body.get("githubUrl");
        if (body.isPresent("portfolioUrl")) student.portfolioUrl = body.get("portfolioUrl");
        if (body.isPresent("resumeUrl")) student.resumeUrl = body.get("resumeUrl");

        student.save();
        return student;
    }
}

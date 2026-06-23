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
import models.sql.User;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

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
        Student student = request.getStudent();
        if (student == null) throw new RoutingError("Student profile not found");

        Request body = request.getRequest();
        User user = request.getUser();

        // ── Academic ───────────────────────────────────────────────────────────
        if (body.isPresent("cgpa"))               student.cgpa               = new BigDecimal(String.valueOf(body.get("cgpa")));
        if (body.isPresent("activeBacklogs"))      student.activeBacklogs     = Integer.parseInt(String.valueOf(body.get("activeBacklogs")));
        if (body.isPresent("totalBacklogs"))       student.totalBacklogs      = Integer.parseInt(String.valueOf(body.get("totalBacklogs")));
        if (body.isPresent("tenthPercentage"))     student.tenthPercentage    = body.get("tenthPercentage");
        if (body.isPresent("twelfthPercentage"))   student.twelfthPercentage  = body.get("twelfthPercentage");
        if (body.isPresent("diplomaPercentage"))   student.diplomaPercentage  = body.get("diplomaPercentage");
        if (body.isPresent("department"))          student.department         = body.get("department");
        if (body.isPresent("passingYear"))         student.passingYear        = Integer.parseInt(String.valueOf(body.get("passingYear")));

        // ── Personal ───────────────────────────────────────────────────────────
        if (body.isPresent("gender"))              student.gender             = body.get("gender");
        if (body.isPresent("dateOfBirth"))         student.dateOfBirth        = body.get("dateOfBirth");
        if (body.isPresent("category"))            student.category           = ((String) body.get("category")).toUpperCase();

        // ── Identity docs ──────────────────────────────────────────────────────
        if (body.isPresent("aadharNumber"))        student.aadharNumber       = body.get("aadharNumber");
        if (body.isPresent("panNumber"))           student.panNumber          = ((String) body.get("panNumber")).toUpperCase();
        if (body.isPresent("studentCollegeId"))    student.studentCollegeId   = body.get("studentCollegeId");

        // ── Social / Online ────────────────────────────────────────────────────
        if (body.isPresent("linkedinUrl"))         student.linkedinUrl        = body.get("linkedinUrl");
        if (body.isPresent("githubUrl"))           student.githubUrl          = body.get("githubUrl");
        if (body.isPresent("portfolioUrl"))        student.portfolioUrl       = body.get("portfolioUrl");

        // ── Skills / Certs ─────────────────────────────────────────────────────
        if (body.isPresent("skills"))              student.skills             = body.get("skills");
        if (body.isPresent("certifications"))      student.certifications     = body.get("certifications");

        // ── User-level fields ──────────────────────────────────────────────────
        if (body.isPresent("mobile")) {
            user.mobile = body.get("mobile");
            user.update();
        }
        if (body.isPresent("name")) {
            user.name = body.get("name");
            user.update();
        }
        if (body.isPresent("avatarUrl")) {
            user.avatarUrl = body.get("avatarUrl");
            user.update();
        }

        student.update();
        return StudentDtos.toProfileDto(student);
    }
}

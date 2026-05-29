package college;

import helpers.annotations.CollegeRole;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.college.CollegeAccessMiddleware;
import models.body.CollegeLoginRequest;
import models.repos.StudentRepository;
import models.services.CsvBuilder;
import models.sql.Student;

import java.util.ArrayList;
import java.util.List;

/**
 * TPO downloads a CSV report of students.
 *
 * GET /reports/students?type=all|placed|unplaced|unverified
 */
@CollegeRole
public enum StudentReportController implements BaseController {

    INSTANCE;

    @Override
    public void handle(RoutingContext event) {
        CollegeAccessMiddleware.INSTANCE.with(event, new ArrayList<>(), this.getClass())
                .map(this::map)
                .subscribe(
                        csv -> writeCsvResponse(event, "students-report.csv", (String) csv),
                        error -> ResponseUtils.INSTANCE.handleError(event, error)
                );
    }

    private Object map(CollegeLoginRequest request) {
        Long collegeId = request.getCollege().getId();
        String type = request.getRoutingContext().queryParams().get("type");
        if (type == null || type.isEmpty()) type = "all";

        List<Student> students;
        switch (type.toLowerCase()) {
            case "placed":
                students = StudentRepository.INSTANCE.findPlaced(collegeId);
                break;
            case "unplaced":
                students = StudentRepository.INSTANCE.findUnplaced(collegeId);
                break;
            case "unverified":
                students = StudentRepository.INSTANCE.where()
                        .eq("college.id", collegeId)
                        .eq("user.verified", false)
                        .findList();
                break;
            default:
                students = StudentRepository.INSTANCE.byCollege(collegeId);
                break;
        }

        CsvBuilder csv = new CsvBuilder();
        csv.header("ID", "Name", "Email", "Mobile", "Enrollment No", "Department",
                "Passing Year", "CGPA", "Active Backlogs", "Total Backlogs",
                "10th %", "12th %", "Diploma %", "Gender", "DOB",
                "Placed", "Current CTC", "Opted Out", "Verified",
                "LinkedIn", "GitHub", "Resume URL");

        for (Student s : students) {
            csv.row(
                    str(s.getId()),
                    s.user != null ? str(s.user.name) : "",
                    s.user != null ? str(s.user.email) : "",
                    s.user != null ? str(s.user.mobile) : "",
                    str(s.enrollmentNumber),
                    str(s.department),
                    str(s.passingYear),
                    s.cgpa != null ? s.cgpa.toPlainString() : "",
                    str(s.activeBacklogs),
                    str(s.totalBacklogs),
                    str(s.tenthPercentage),
                    str(s.twelfthPercentage),
                    str(s.diplomaPercentage),
                    str(s.gender),
                    str(s.dateOfBirth),
                    str(s.placed),
                    s.currentCtc != null ? s.currentCtc.toPlainString() : "",
                    str(s.optedOut),
                    s.user != null ? str(s.user.verified) : "false",
                    str(s.linkedinUrl),
                    str(s.githubUrl),
                    str(s.resumeUrl)
            );
        }

        return csv.build();
    }

    private void writeCsvResponse(RoutingContext event, String filename, String csv) {
        if (event.response().closed()) return;
        event.response()
                .putHeader("Content-Type", "text/csv; charset=utf-8")
                .putHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .end(csv);
    }

    private String str(Object value) {
        return value != null ? value.toString() : "";
    }
}

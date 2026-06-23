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
import models.sql.User;

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
            User u = s.getUser();
            csv.row(
                    str(s.getId()),
                    u != null ? str(u.getName()) : "",
                    u != null ? str(u.getEmail()) : "",
                    u != null ? str(u.getMobile()) : "",
                    str(s.getEnrollmentNumber()),
                    str(s.getDepartment()),
                    str(s.getPassingYear()),
                    s.getCgpa() != null ? s.getCgpa().toPlainString() : "",
                    str(s.getActiveBacklogs()),
                    str(s.getTotalBacklogs()),
                    str(s.getTenthPercentage()),
                    str(s.getTwelfthPercentage()),
                    str(s.getDiplomaPercentage()),
                    str(s.getGender()),
                    str(s.getDateOfBirth()),
                    str(s.isPlaced()),
                    s.getCurrentCtc() != null ? s.getCurrentCtc().toPlainString() : "",
                    str(s.isOptedOut()),
                    u != null ? str(u.isVerified()) : "false",
                    str(s.getLinkedinUrl()),
                    str(s.getGithubUrl()),
                    str(s.getResumeUrl())
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

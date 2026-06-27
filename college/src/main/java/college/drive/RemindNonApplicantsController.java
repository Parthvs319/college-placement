package college.drive;

import helpers.annotations.CollegeRole;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.college.CollegeAccessMiddleware;
import models.body.CollegeLoginRequest;
import models.repos.DriveApplicationRepository;
import models.repos.DriveRepository;
import models.repos.StudentRepository;
import models.services.EmailService;
import models.sql.College;
import models.sql.Drive;
import models.sql.Student;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * POST /college/drives/:driveId/remind-non-applicants
 * Sends a reminder email to all students who have NOT yet applied for a drive.
 */
@CollegeRole
public enum RemindNonApplicantsController implements BaseController {

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
        College college = request.getCollege();

        String driveIdStr = request.getRoutingContext().pathParam("driveId");
        if (driveIdStr == null || driveIdStr.isEmpty()) {
            throw new RoutingError("driveId path param is required");
        }
        long driveId = Long.parseLong(driveIdStr);

        Drive drive = DriveRepository.INSTANCE.byId(driveId);
        if (drive == null) throw new RoutingError(404, "Drive not found");

        // Verify drive belongs to this college
        if (drive.getCompanyCollege() == null
                || drive.getCompanyCollege().getCollege() == null
                || !drive.getCompanyCollege().getCollege().getId().equals(college.getId())) {
            throw new RoutingError(403, "Drive does not belong to your college");
        }

        String companyName = drive.getCompanyCollege().getCompany() != null
                ? drive.getCompanyCollege().getCompany().getName()
                : "Company";
        String driveTitle = drive.getTitle();
        String deadline = drive.getRegistrationDeadline() != null
                ? drive.getRegistrationDeadline().toLocalDateTime().toLocalDate().toString()
                : "soon";

        // All students in college
        List<Student> allStudents = StudentRepository.INSTANCE.byCollege(college.getId());

        // Applicant studentIds
        Set<Long> applicantIds = DriveApplicationRepository.INSTANCE.byDrive(driveId)
                .stream()
                .map(a -> a.getStudent().getId())
                .collect(Collectors.toSet());

        // Non-applicants
        List<Student> nonApplicants = allStudents.stream()
                .filter(s -> !applicantIds.contains(s.getId()))
                .collect(Collectors.toList());

        String subject = "Reminder: Apply for " + companyName + " – " + driveTitle + " before " + deadline;

        for (Student student : nonApplicants) {
            String studentName = student.getUser() != null && student.getUser().getName() != null
                    ? student.getUser().getName()
                    : "Student";
            String toEmail = student.getUser() != null ? student.getUser().getEmail() : null;
            if (toEmail == null || toEmail.isEmpty()) continue;

            String html = "<p>Dear " + studentName + ",</p>"
                    + "<p>This is a friendly reminder that <strong>" + companyName + "</strong> is conducting a placement drive "
                    + "at your college for the role of <strong>" + driveTitle + "</strong>.</p>"
                    + "<p>The registration deadline is <strong>" + deadline + "</strong>. Don't miss this opportunity!</p>"
                    + "<p>Apply now through <strong>Applyra</strong> to secure your spot.</p>"
                    + "<p>Best regards,<br/>Placement Cell<br/>" + college.getName() + "</p>";

            EmailService.sendEmail(toEmail, subject, html)
                    .subscribe(ok -> {}, err -> System.err.println("[RemindNonApplicants] Email error: " + err.getMessage()));
        }

        Map<String, Object> result = new HashMap<>();
        result.put("message", "Reminder sent to " + nonApplicants.size() + " students");
        result.put("count", nonApplicants.size());
        result.put("driveTitle", driveTitle);
        result.put("deadline", deadline);
        return result;
    }
}

package college.student;

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
import models.sql.DriveApplication;
import models.sql.Student;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * POST /college/students/warn
 * Body: { "type": "OA_CHEATING" | "CONSECUTIVE_SKIP", "studentIds": [1,2,3], "reason": "optional" }
 */
@CollegeRole
public enum WarnStudentsController implements BaseController {

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

        String type = request.getRequest().get("type");
        if (type == null || type.isEmpty()) {
            throw new RoutingError("type is required (OA_CHEATING or CONSECUTIVE_SKIP)");
        }

        String reason = request.getRequest().isPresent("reason") ? request.getRequest().get("reason") : null;

        List<Student> targets;

        switch (type.toUpperCase()) {
            case "OA_CHEATING": {
                Object idsObj = request.getRequest().get("studentIds");
                if (!(idsObj instanceof List) || ((List<?>) idsObj).isEmpty()) {
                    throw new RoutingError("studentIds is required for OA_CHEATING");
                }
                List<Long> studentIds = ((List<?>) idsObj).stream()
                        .map(o -> Long.parseLong(o.toString()))
                        .collect(Collectors.toList());

                targets = studentIds.stream()
                        .map(StudentRepository.INSTANCE::byId)
                        .filter(s -> s != null && s.getCollege().getId().equals(college.getId()))
                        .collect(Collectors.toList());

                for (Student student : targets) {
                    sendOaCheatingWarning(student, college, reason);
                }
                break;
            }

            case "CONSECUTIVE_SKIP": {
                // Check if explicit studentIds provided, otherwise auto-detect
                if (request.getRequest().isPresent("studentIds")) {
                    Object idsObj = request.getRequest().get("studentIds");
                    if (idsObj instanceof List && !((List<?>) idsObj).isEmpty()) {
                        List<Long> studentIds = ((List<?>) idsObj).stream()
                                .map(o -> Long.parseLong(o.toString()))
                                .collect(Collectors.toList());
                        targets = studentIds.stream()
                                .map(StudentRepository.INSTANCE::byId)
                                .filter(s -> s != null && s.getCollege().getId().equals(college.getId()))
                                .collect(Collectors.toList());
                    } else {
                        targets = findConsecutiveSkippers(college);
                    }
                } else {
                    targets = findConsecutiveSkippers(college);
                }

                for (Student student : targets) {
                    sendConsecutiveSkipWarning(student, college);
                }
                break;
            }

            default:
                throw new RoutingError("Invalid type: " + type + ". Use OA_CHEATING or CONSECUTIVE_SKIP");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("message", "Warning sent to " + targets.size() + " students");
        result.put("count", targets.size());
        result.put("type", type.toUpperCase());
        return result;
    }

    private List<Student> findConsecutiveSkippers(College college) {
        // Get all drives for this college ordered by date (most recent last)
        List<Drive> collegeDrives = DriveRepository.INSTANCE.byCollege(college.getId());
        // byCollege returns desc by driveDate; reverse to get asc order
        List<Drive> drivesAsc = new ArrayList<>(collegeDrives);
        java.util.Collections.reverse(drivesAsc);

        if (drivesAsc.size() < 3) return new ArrayList<>();

        // Last 5 drives (or fewer if not enough)
        int window = Math.min(5, drivesAsc.size());
        List<Drive> recentDrives = drivesAsc.subList(drivesAsc.size() - window, drivesAsc.size());

        List<Student> allStudents = StudentRepository.INSTANCE.byCollege(college.getId());
        List<Student> skippers = new ArrayList<>();

        for (Student student : allStudents) {
            Set<Long> appliedDriveIds = DriveApplicationRepository.INSTANCE.byStudent(student.getId())
                    .stream()
                    .map(a -> a.getDrive().getId())
                    .collect(Collectors.toSet());

            // Check last 3 consecutive skips (from the most recent end)
            int consecutiveSkips = 0;
            for (int i = recentDrives.size() - 1; i >= 0; i--) {
                if (!appliedDriveIds.contains(recentDrives.get(i).getId())) {
                    consecutiveSkips++;
                } else {
                    break;
                }
            }

            if (consecutiveSkips >= 3) {
                skippers.add(student);
            }
        }
        return skippers;
    }

    private void sendOaCheatingWarning(Student student, College college, String reason) {
        String toEmail = student.getUser() != null ? student.getUser().getEmail() : null;
        if (toEmail == null || toEmail.isEmpty()) return;

        String studentName = student.getUser().getName() != null ? student.getUser().getName() : "Student";
        String subject = "Warning: Unfair Practice Detected During Online Assessment";
        String html = "<p>Dear " + studentName + ",</p>"
                + "<p>We have been informed that you may have engaged in <strong>unfair practices</strong> "
                + "during the Online Assessment (OA) for a recent campus placement drive.</p>"
                + (reason != null && !reason.isEmpty()
                        ? "<p><strong>Details:</strong> " + reason + "</p>"
                        : "")
                + "<p>Such behaviour is a serious violation of our placement code of conduct. "
                + "Please be advised that repeated violations may result in permanent disqualification "
                + "from placement activities.</p>"
                + "<p>We urge you to maintain integrity in all future assessments.</p>"
                + "<p>Regards,<br/>Placement Cell<br/>" + college.getName() + "</p>";

        EmailService.sendEmail(toEmail, subject, html)
                .subscribe(ok -> {}, err -> System.err.println("[WarnStudents] Email error: " + err.getMessage()));
    }

    private void sendConsecutiveSkipWarning(Student student, College college) {
        String toEmail = student.getUser() != null ? student.getUser().getEmail() : null;
        if (toEmail == null || toEmail.isEmpty()) return;

        String studentName = student.getUser().getName() != null ? student.getUser().getName() : "Student";
        String subject = "Action Required: Missed Consecutive Placement Drives";
        String html = "<p>Dear " + studentName + ",</p>"
                + "<p>We noticed that you have <strong>not applied to 3 or more consecutive campus placement drives</strong> "
                + "at " + college.getName() + ".</p>"
                + "<p>Regular participation in placement drives is important for your career. "
                + "Please log in to <strong>Applyra</strong> and check the available drives.</p>"
                + "<p>If you have opted out of placements or have any concerns, please contact your Placement Officer.</p>"
                + "<p>Regards,<br/>Placement Cell<br/>" + college.getName() + "</p>";

        EmailService.sendEmail(toEmail, subject, html)
                .subscribe(ok -> {}, err -> System.err.println("[WarnStudents] Email error: " + err.getMessage()));
    }
}

package college.drive;

import helpers.annotations.CollegeRole;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.college.CollegeAccessMiddleware;
import models.body.CollegeLoginRequest;
import models.repos.DriveRepository;
import models.repos.StudentRepository;
import models.services.EmailService;
import models.sql.Drive;
import models.sql.Student;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * POST /college/drives/:driveId/finalize-venue
 * Body: { venue }
 *
 * Sets the venue on the drive and emails all eligible students.
 */
@CollegeRole
public enum FinalizeVenueController implements BaseController {

    INSTANCE;

    @Override
    public void handle(RoutingContext event) {
        CollegeAccessMiddleware.INSTANCE.with(event, new ArrayList<>(), this.getClass())
                .map(req -> map(req, event))
                .subscribe(
                        o -> ResponseUtils.INSTANCE.writeJsonResponse(event, o),
                        error -> ResponseUtils.INSTANCE.handleError(event, error)
                );
    }

    private Object map(CollegeLoginRequest request, RoutingContext event) {
        String idParam = event.pathParam("driveId");
        Drive drive = DriveRepository.INSTANCE.byId(Long.parseLong(idParam));
        if (drive == null) throw new RoutingError("Drive not found");

        JsonObject body = event.body().asJsonObject();
        if (body == null) body = new JsonObject();

        String venue = body.getString("venue");
        if (venue == null || venue.isBlank()) throw new RoutingError("Venue is required");

        drive.setVenue(venue.trim());
        drive.update();

        // Send venue notification to eligible students in background
        final String finalVenue = venue.trim();
        final Long collegeId = request.getCollege().getId();
        final String collegeName = request.getCollege().getName();
        final String driveTitle = drive.getTitle();
        final String companyName = drive.getCompanyCollege() != null && drive.getCompanyCollege().getCompany() != null
                ? drive.getCompanyCollege().getCompany().getName() : "";
        final String driveDate = drive.getDriveDate() != null ? drive.getDriveDate().toString().substring(0, 10) : "TBD";
        final String tpoName = request.getUser().getName();
        final String tpoEmail = request.getUser().getEmail();

        // Build eligibility params from drive
        final java.math.BigDecimal minCgpa = drive.getMinCgpa();
        final int maxBacklogs = drive.getMaxActiveBacklogs();
        final List<String> departments = drive.getEligibleDepartments();
        final int passingYear = drive.getAcademicYear();

        new Thread(() -> {
            try {
                List<Student> students = StudentRepository.INSTANCE.findEligible(
                        collegeId,
                        minCgpa != null ? minCgpa : java.math.BigDecimal.ZERO,
                        maxBacklogs,
                        departments,
                        passingYear
                );

                String subject = "Venue Finalized: " + driveTitle + " | " + collegeName;

                for (Student s : students) {
                    if (s.getUser() == null || s.getUser().getEmail() == null) continue;

                    String html = buildVenueEmailHtml(
                            s.getUser().getName(),
                            driveTitle, companyName, finalVenue, driveDate, collegeName
                    );

                    EmailService.sendEmailWithReplyTo(
                            s.getUser().getEmail(),
                            s.getUser().getName(),
                            subject, html,
                            tpoEmail, tpoName,
                            null
                    ).subscribe(
                            ok -> {},
                            err -> System.err.println("[FinalizeVenue] Email error: " + err.getMessage())
                    );

                    Thread.sleep(80);
                }

                System.out.println("[FinalizeVenue] Notified " + students.size() + " students about venue for drive " + driveTitle);
            } catch (Exception e) {
                System.err.println("[FinalizeVenue] Thread error: " + e.getMessage());
            }
        }, "finalize-venue-email").start();

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("message", "Venue finalized and students are being notified");
        res.put("venue", finalVenue);
        return res;
    }

    private static String buildVenueEmailHtml(
            String studentName, String driveTitle, String companyName,
            String venue, String driveDate, String collegeName) {

        return "<!DOCTYPE html><html><body style='font-family:sans-serif;color:#1a1a1a;max-width:600px;margin:0 auto;padding:20px'>"
                + "<h2 style='color:#7c3aed'>Venue Finalized</h2>"
                + "<p>Hi " + esc(studentName) + ",</p>"
                + "<p>The venue for <strong>" + esc(driveTitle) + "</strong>"
                + (companyName != null && !companyName.isEmpty() ? " (" + esc(companyName) + ")" : "")
                + " has been finalized.</p>"
                + "<table style='border-collapse:collapse;width:100%;margin:16px 0'>"
                + row("Venue", venue)
                + row("Drive Date", driveDate)
                + row("Drive", driveTitle)
                + "</table>"
                + "<p>Please be at the venue on time. For any queries, reply to this email.</p>"
                + "<p style='margin-top:24px;color:#6b7280;font-size:12px'>- " + esc(collegeName) + " via Applyra</p>"
                + "</body></html>";
    }

    private static String row(String label, String value) {
        return "<tr><td style='padding:6px 12px;border:1px solid #e5e7eb;background:#f9fafb;font-weight:600'>"
                + esc(label) + "</td><td style='padding:6px 12px;border:1px solid #e5e7eb'>" + esc(value) + "</td></tr>";
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}

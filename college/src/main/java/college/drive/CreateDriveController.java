package college.drive;

import helpers.annotations.CollegeRole;
import helpers.blueprint.enums.RequestItemType;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.Request;
import helpers.utils.RequestItem;
import helpers.utils.ResponseUtils;
import helpers.utils.SuccessResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.college.CollegeAccessMiddleware;
import models.body.CollegeLoginRequest;
import models.enums.DriveStatus;
import models.enums.EmploymentType;
import models.enums.RoundType;
import models.enums.UserType;
import models.json.CollegeDtos;
import models.repos.CompanyCollegeRepository;
import models.repos.DriveRepository;
import models.repos.StudentRepository;
import models.repos.UserRepository;
import models.services.EmailService;
import models.sql.CompanyCollege;
import models.sql.Drive;
import models.sql.DriveRound;
import models.sql.Student;
import models.sql.User;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@CollegeRole
public enum CreateDriveController implements BaseController {

    INSTANCE;

    public List<RequestItem> items() {
        List<RequestItem> items = new ArrayList<>();
        items.add(RequestItem.builder().key("companyCollegeId").itemType(RequestItemType.INTEGER).required(true).build());
        items.add(RequestItem.builder().key("title").itemType(RequestItemType.STRING).required(true).build());
        items.add(RequestItem.builder().key("employmentType").itemType(RequestItemType.STRING).required(true).build());
        items.add(RequestItem.builder().key("jobDescription").itemType(RequestItemType.STRING).required(false).build());
        items.add(RequestItem.builder().key("jdFileUrl").itemType(RequestItemType.STRING).required(false).build());
        items.add(RequestItem.builder().key("academicYear").itemType(RequestItemType.INTEGER).required(false).build());
        items.add(RequestItem.builder().key("minCgpa").itemType(RequestItemType.DOUBLE).required(false).build());
        items.add(RequestItem.builder().key("maxActiveBacklogs").itemType(RequestItemType.INTEGER).required(false).build());
        items.add(RequestItem.builder().key("ctcOffered").itemType(RequestItemType.DOUBLE).required(false).build());
        items.add(RequestItem.builder().key("stipend").itemType(RequestItemType.DOUBLE).required(false).build());
        items.add(RequestItem.builder().key("location").itemType(RequestItemType.STRING).required(false).build());
        items.add(RequestItem.builder().key("isRemote").itemType(RequestItemType.BOOLEAN).required(false).build());
        items.add(RequestItem.builder().key("venue").itemType(RequestItemType.STRING).required(false).build());
        items.add(RequestItem.builder().key("registrationDeadline").itemType(RequestItemType.STRING).required(false).build());
        items.add(RequestItem.builder().key("driveDate").itemType(RequestItemType.STRING).required(false).build());
        items.add(RequestItem.builder().key("minPassingYear").itemType(RequestItemType.INTEGER).required(false).build());
        items.add(RequestItem.builder().key("maxPassingYear").itemType(RequestItemType.INTEGER).required(false).build());
        items.add(RequestItem.builder().key("eligibleDepartments").itemType(RequestItemType.ARRAY).required(false).build());
        items.add(RequestItem.builder().key("rounds").itemType(RequestItemType.ARRAY).required(false).build());
        items.add(RequestItem.builder().key("notifyEligibleStudents").itemType(RequestItemType.BOOLEAN).required(false).build());
        items.add(RequestItem.builder().key("notifyCompanyHR").itemType(RequestItemType.BOOLEAN).required(false).build());
        items.add(RequestItem.builder().key("notifyAdmins").itemType(RequestItemType.BOOLEAN).required(false).build());
        return items;
    }

    @Override
    public void handle(RoutingContext event) {
        CollegeAccessMiddleware.INSTANCE.with(event, items(), this.getClass())
                .map(this::createDrive)
                .subscribe(
                        o -> ResponseUtils.INSTANCE.writeJsonResponse(event, o),
                        error -> ResponseUtils.INSTANCE.handleError(event, error)
                );
    }

    private Object createDrive(CollegeLoginRequest request) {
        Request body = request.getRequest();

        Long companyCollegeId = body.get("companyCollegeId");
        String title = body.get("title");
        String employmentTypeStr = body.get("employmentType");
        CompanyCollege cc = CompanyCollegeRepository.INSTANCE.byId(companyCollegeId);
        if (cc == null) {
            throw new RoutingError("Company-College link not found");
        }

        if (!cc.getCollege().getId().equals(request.getCollege().getId())) {
            throw new RoutingError("You can only create drives for your own college");
        }

        Drive drive = new Drive();
        drive.companyCollege = cc;
        drive.title = title;
        drive.employmentType = EmploymentType.valueOf(employmentTypeStr);
        drive.status = DriveStatus.UPCOMING;

        if (body.isPresent("jobDescription")) drive.jobDescription = body.get("jobDescription");
        if (body.isPresent("jdFileUrl")) drive.jdFileUrl = body.get("jdFileUrl");
        if (body.isPresent("academicYear")) {
            Long ay = body.get("academicYear");
            if (ay != null) drive.academicYear = ay.intValue();
        }
        if (body.isPresent("minCgpa")) {
            Double mc = body.get("minCgpa");
            if (mc != null) drive.minCgpa = BigDecimal.valueOf(mc);
        }
        if (body.isPresent("maxActiveBacklogs")) {
            Long mab = body.get("maxActiveBacklogs");
            if (mab != null) drive.maxActiveBacklogs = mab.intValue();
        }
        if (body.isPresent("ctcOffered")) {
            Double ctc = body.get("ctcOffered");
            if (ctc != null) drive.ctcOffered = BigDecimal.valueOf(ctc);
        }
        if (body.isPresent("stipend")) {
            Double st = body.get("stipend");
            if (st != null) drive.stipend = BigDecimal.valueOf(st);
        }
        if (body.isPresent("location")) drive.location = body.get("location");
        if (body.isPresent("isRemote")) {
            Boolean ir = body.get("isRemote");
            if (ir != null) drive.isRemote = ir;
        }
        if (body.isPresent("venue")) drive.venue = body.get("venue");
        if (body.isPresent("registrationDeadline")) {
            String rd = body.get("registrationDeadline");
            if (rd != null) drive.registrationDeadline = Timestamp.valueOf(rd);
        }
        if (body.isPresent("driveDate")) {
            String dd = body.get("driveDate");
            if (dd != null) drive.driveDate = Timestamp.valueOf(dd);
        }
        if (body.isPresent("minPassingYear")) {
            Long mpy = body.get("minPassingYear");
            if (mpy != null) drive.minPassingYear = mpy.intValue();
        }
        if (body.isPresent("maxPassingYear")) {
            Long mxpy = body.get("maxPassingYear");
            if (mxpy != null) drive.maxPassingYear = mxpy.intValue();
        }

        // Eligible departments from ARRAY
        if (body.isPresent("eligibleDepartments")) {
            JsonArray deptsArr = body.get("eligibleDepartments");
            if (deptsArr != null && !deptsArr.isEmpty()) {
                List<String> depts = new ArrayList<>();
                for (int i = 0; i < deptsArr.size(); i++) {
                    depts.add(deptsArr.getString(i));
                }
                drive.eligibleDepartments = depts;
            }
        }

        drive.save(request.getCollege() , cc);

        if (body.isPresent("rounds")) {
            JsonArray roundsArr = body.get("rounds");
            if (roundsArr != null) {
                for (int i = 0; i < roundsArr.size(); i++) {
                    JsonObject rObj = roundsArr.getJsonObject(i);
                    DriveRound round = new DriveRound();
                    round.drive = drive;
                    round.roundNumber = rObj.getInteger("roundNumber", i + 1);
                    round.roundType = RoundType.valueOf(rObj.getString("roundType", "OTHER"));
                    if (rObj.containsKey("name") && rObj.getString("name") != null)
                        round.name = rObj.getString("name");
                    if (rObj.containsKey("description") && rObj.getString("description") != null)
                        round.description = rObj.getString("description");
                    if (rObj.containsKey("venue") && rObj.getString("venue") != null)
                        round.venue = rObj.getString("venue");
                    if (rObj.containsKey("durationMinutes") && rObj.getInteger("durationMinutes") != null)
                        round.durationMinutes = rObj.getInteger("durationMinutes");
                    if (rObj.containsKey("scheduledAt") && rObj.getString("scheduledAt") != null) {
                        try { round.scheduledAt = Timestamp.valueOf(rObj.getString("scheduledAt")); }
                        catch (Exception ignored) {}
                    }
                    round.save();
                }
            }
        }

        Boolean notifyStudents = body.isPresent("notifyEligibleStudents") ? body.get("notifyEligibleStudents") : false;
        Boolean notifyHR       = body.isPresent("notifyCompanyHR") ? body.get("notifyCompanyHR") : false;
        Boolean notifyAdmins   = body.isPresent("notifyAdmins") ? body.get("notifyAdmins") : false;
        if (notifyStudents == null) notifyStudents = false;
        if (notifyHR == null) notifyHR = false;
        if (notifyAdmins == null) notifyAdmins = false;

        if (notifyStudents || notifyHR || notifyAdmins) {
            final String collegeName = request.getCollege().getName() != null
                    ? request.getCollege().getName() : "Your College";
            final String companyName = cc.getCompany() != null ? cc.getCompany().getName() : "Company";
            final String driveTitle = drive.getTitle();
            final String driveCode = drive.getDriveCode();
            final String driveDateStr = drive.getDriveDate() != null ? drive.getDriveDate().toString() : "TBD";
            final String regDeadlineStr = drive.getRegistrationDeadline() != null
                    ? drive.getRegistrationDeadline().toString() : "TBD";
            final String ctcStr = drive.getCtcOffered() != null
                    ? "Rs. " + drive.getCtcOffered().divide(new BigDecimal(100000)).toPlainString() + " LPA"
                    : "Not specified";
            final String locationStr = drive.isRemote()
                    ? (drive.getLocation() != null ? "Hybrid - " + drive.getLocation() : "Online")
                    : (drive.getLocation() != null ? drive.getLocation() : "TBD");
            final Long collegeId = request.getCollege().getId();
            final String tpoEmail = request.getUser().getEmail();
            final String tpoName = request.getUser().getName();
            final BigDecimal minCgpa = drive.getMinCgpa();
            final int maxBacklogs = drive.getMaxActiveBacklogs();
            final List<String> deptFilter = drive.getEligibleDepartments();
            final int passYear = drive.getAcademicYear();

            final boolean doNotifyStudents = notifyStudents;
            final boolean doNotifyHR = notifyHR;
            final boolean doNotifyAdmins = notifyAdmins;

            new Thread(() -> {
                int sentCount = 0;

                if (doNotifyStudents) {
                    try {
                        List<Student> eligible = StudentRepository.INSTANCE.findEligible(
                                collegeId,
                                minCgpa != null ? minCgpa : BigDecimal.ZERO,
                                maxBacklogs, deptFilter, passYear
                        );

                        for (Student s : eligible) {
                            try {
                                String studentName = s.getUser() != null ? s.getUser().getName() : "";
                                String studentEmail = s.getUser() != null ? s.getUser().getEmail() : null;
                                if (studentEmail == null || studentEmail.isBlank()) continue;

                                String html = buildDriveStudentEmail(
                                        studentName, companyName, driveTitle, driveCode,
                                        ctcStr, locationStr, driveDateStr, regDeadlineStr,
                                        collegeName
                                );
                                EmailService.sendEmailWithReplyTo(
                                        studentEmail, studentName,
                                        "New Drive: " + companyName + " - " + driveTitle,
                                        html, tpoEmail, tpoName, new ArrayList<>()
                                ).subscribe(ok -> {}, err -> {});
                                sentCount++;
                                Thread.sleep(80);
                            } catch (Exception e) {
                                System.err.println("[DriveNotify] Student error: " + e.getMessage());
                            }
                        }
                        System.out.println("[DriveNotify] Sent to " + sentCount + " eligible students");
                    } catch (Exception e) {
                        System.err.println("[DriveNotify] findEligible error: " + e.getMessage());
                    }
                }

                if (doNotifyHR) {
                    try {
                        User hrUser = cc.getManagedByUser();
                        if (hrUser != null && hrUser.getEmail() != null && !hrUser.getEmail().isBlank()) {
                            String html = buildDriveHREmail(
                                    hrUser.getName(), companyName, driveTitle, driveCode,
                                    ctcStr, locationStr, driveDateStr, regDeadlineStr,
                                    collegeName, tpoName, tpoEmail
                            );
                            EmailService.sendEmail(
                                    hrUser.getEmail(),
                                    "Drive Created: " + driveTitle + " at " + collegeName,
                                    html
                            ).subscribe(ok -> {}, err -> {});
                        }
                    } catch (Exception e) {
                        System.err.println("[DriveNotify] HR error: " + e.getMessage());
                    }
                }

                if (doNotifyAdmins) {
                    try {
                        List<User> superAdmins = UserRepository.INSTANCE.findByUserType(UserType.SUPER_ADMIN);
                        for (User sa : superAdmins) {
                            if (sa.getEmail() == null || sa.getEmail().isBlank()) continue;
                            String html = buildDriveAdminEmail(
                                    sa.getName(), companyName, driveTitle, driveCode,
                                    ctcStr, locationStr, driveDateStr, collegeName, tpoName
                            );
                            EmailService.sendEmail(
                                    sa.getEmail(),
                                    "[Admin] New Drive: " + companyName + " at " + collegeName,
                                    html
                            ).subscribe(ok -> {}, err -> {});
                            try { Thread.sleep(80); } catch (InterruptedException ignored) {}
                        }

                        List<User> tpos = UserRepository.INSTANCE.byCollegeAndType(collegeId, UserType.TPO);
                        for (User tpo : tpos) {
                            if (tpo.getEmail() == null || tpo.getEmail().isBlank()) continue;
                            if (tpo.getEmail().equals(tpoEmail)) continue;
                            String html = buildDriveAdminEmail(
                                    tpo.getName(), companyName, driveTitle, driveCode,
                                    ctcStr, locationStr, driveDateStr, collegeName, tpoName
                            );
                            EmailService.sendEmail(
                                    tpo.getEmail(),
                                    "New Drive Created: " + companyName + " - " + driveTitle,
                                    html
                            ).subscribe(ok -> {}, err -> {});
                            try { Thread.sleep(80); } catch (InterruptedException ignored) {}
                        }
                    } catch (Exception e) {
                        System.err.println("[DriveNotify] Admin/TPO error: " + e.getMessage());
                    }
                }

                System.out.println("[DriveNotify] Done. drive=" + driveCode + " college=" + collegeName);
            }, "drive-create-notify").start();
        }

        return CollegeDtos.toDriveDto(drive);
    }

    private String buildDriveStudentEmail(String studentName, String companyName,
                                           String driveTitle, String driveCode,
                                           String ctc, String location,
                                           String driveDate, String regDeadline,
                                           String collegeName) {
        String greeting = (studentName != null && !studentName.isBlank())
                ? "Hi " + studentName.split("\\s+")[0] + "," : "Hello,";

        return "<!DOCTYPE html>"
                + "<html><head><meta charset='UTF-8'></head>"
                + "<body style='margin:0;padding:0;background:#F3F4F6;font-family:-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Arial,sans-serif'>"
                + "<table width='100%' cellpadding='0' cellspacing='0' style='background:#F3F4F6;padding:28px 16px'>"
                + "<tr><td align='center'>"
                + "<table width='600' cellpadding='0' cellspacing='0' style='max-width:600px;width:100%'>"
                + "<tr><td style='background:#1A1A2E;border-radius:12px 12px 0 0;padding:24px 32px'>"
                + "<span style='color:#C4A55A;font-size:20px;font-weight:800'>Applyra</span>"
                + "<span style='color:#9CA3AF;font-size:12px;margin-left:10px'>" + esc(collegeName) + "</span>"
                + "</td></tr>"
                + "<tr><td style='background:#fff;padding:32px;border:1px solid #E5E7EB;border-top:none'>"
                + "<p style='margin:0 0 16px;font-size:15px;font-weight:600;color:#111827'>" + greeting + "</p>"
                + "<p style='font-size:14px;color:#374151;line-height:1.7;margin:0 0 16px'>"
                + "A new placement drive has been announced. You are eligible to apply!</p>"
                + "<table style='width:100%;border-collapse:collapse;margin:16px 0'>"
                + row("Company", esc(companyName))
                + row("Role", esc(driveTitle) + " <span style='color:#9CA3AF;font-size:11px'>(" + esc(driveCode) + ")</span>")
                + row("CTC", esc(ctc))
                + row("Location", esc(location))
                + row("Drive Date", esc(driveDate))
                + row("Apply By", esc(regDeadline))
                + "</table>"
                + "<p style='font-size:13px;color:#6B7280;margin:16px 0 0'>Log in to your Applyra portal to apply.</p>"
                + "</td></tr>"
                + "<tr><td style='background:#F9FAFB;border-radius:0 0 12px 12px;border:1px solid #E5E7EB;border-top:none;padding:16px 32px;text-align:center'>"
                + "<p style='margin:0;color:#9CA3AF;font-size:11px'>Sent via Applyra on behalf of " + esc(collegeName) + " Placement Cell</p>"
                + "</td></tr></table>"
                + "</td></tr></table></body></html>";
    }

    private String buildDriveHREmail(String hrName, String companyName,
                                      String driveTitle, String driveCode,
                                      String ctc, String location,
                                      String driveDate, String regDeadline,
                                      String collegeName, String tpoName, String tpoEmail) {
        String greeting = (hrName != null && !hrName.isBlank())
                ? "Hi " + hrName.split("\\s+")[0] + "," : "Hello,";

        return "<!DOCTYPE html>"
                + "<html><head><meta charset='UTF-8'></head>"
                + "<body style='margin:0;padding:0;background:#F3F4F6;font-family:-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Arial,sans-serif'>"
                + "<table width='100%' cellpadding='0' cellspacing='0' style='background:#F3F4F6;padding:28px 16px'>"
                + "<tr><td align='center'>"
                + "<table width='600' cellpadding='0' cellspacing='0' style='max-width:600px;width:100%'>"
                + "<tr><td style='background:#1A1A2E;border-radius:12px 12px 0 0;padding:24px 32px'>"
                + "<span style='color:#C4A55A;font-size:20px;font-weight:800'>Applyra</span>"
                + "</td></tr>"
                + "<tr><td style='background:#fff;padding:32px;border:1px solid #E5E7EB;border-top:none'>"
                + "<p style='margin:0 0 16px;font-size:15px;font-weight:600;color:#111827'>" + greeting + "</p>"
                + "<p style='font-size:14px;color:#374151;line-height:1.7;margin:0 0 16px'>"
                + "A placement drive has been created for <strong>" + esc(companyName) + "</strong> at <strong>" + esc(collegeName) + "</strong>.</p>"
                + "<table style='width:100%;border-collapse:collapse;margin:16px 0'>"
                + row("Drive", esc(driveTitle) + " (" + esc(driveCode) + ")")
                + row("CTC", esc(ctc))
                + row("Location", esc(location))
                + row("Drive Date", esc(driveDate))
                + row("Reg. Deadline", esc(regDeadline))
                + "</table>"
                + "<p style='font-size:13px;color:#6B7280;margin:16px 0 0'>Contact: " + esc(tpoName) + " (" + esc(tpoEmail) + ")</p>"
                + "</td></tr>"
                + "<tr><td style='background:#F9FAFB;border-radius:0 0 12px 12px;border:1px solid #E5E7EB;border-top:none;padding:16px 32px;text-align:center'>"
                + "<p style='margin:0;color:#9CA3AF;font-size:11px'>Sent via Applyra</p>"
                + "</td></tr></table>"
                + "</td></tr></table></body></html>";
    }

    private String buildDriveAdminEmail(String recipientName, String companyName,
                                         String driveTitle, String driveCode,
                                         String ctc, String location,
                                         String driveDate, String collegeName,
                                         String createdBy) {
        String greeting = (recipientName != null && !recipientName.isBlank())
                ? "Hi " + recipientName.split("\\s+")[0] + "," : "Hello,";

        return "<!DOCTYPE html>"
                + "<html><head><meta charset='UTF-8'></head>"
                + "<body style='margin:0;padding:0;background:#F3F4F6;font-family:-apple-system,BlinkMacSystemFont,Segoe UI,Roboto,Arial,sans-serif'>"
                + "<table width='100%' cellpadding='0' cellspacing='0' style='background:#F3F4F6;padding:28px 16px'>"
                + "<tr><td align='center'>"
                + "<table width='600' cellpadding='0' cellspacing='0' style='max-width:600px;width:100%'>"
                + "<tr><td style='background:#1A1A2E;border-radius:12px 12px 0 0;padding:24px 32px'>"
                + "<span style='color:#C4A55A;font-size:20px;font-weight:800'>Applyra</span>"
                + "</td></tr>"
                + "<tr><td style='background:#fff;padding:32px;border:1px solid #E5E7EB;border-top:none'>"
                + "<p style='margin:0 0 16px;font-size:15px;font-weight:600;color:#111827'>" + greeting + "</p>"
                + "<p style='font-size:14px;color:#374151;line-height:1.7;margin:0 0 16px'>"
                + "A new placement drive has been created.</p>"
                + "<table style='width:100%;border-collapse:collapse;margin:16px 0'>"
                + row("College", esc(collegeName))
                + row("Company", esc(companyName))
                + row("Drive", esc(driveTitle) + " (" + esc(driveCode) + ")")
                + row("CTC", esc(ctc))
                + row("Location", esc(location))
                + row("Drive Date", esc(driveDate))
                + row("Created By", esc(createdBy))
                + "</table>"
                + "</td></tr>"
                + "<tr><td style='background:#F9FAFB;border-radius:0 0 12px 12px;border:1px solid #E5E7EB;border-top:none;padding:16px 32px;text-align:center'>"
                + "<p style='margin:0;color:#9CA3AF;font-size:11px'>Sent via Applyra</p>"
                + "</td></tr></table>"
                + "</td></tr></table></body></html>";
    }

    private static String row(String label, String value) {
        return "<tr><td style='padding:8px 12px;border-bottom:1px solid #F3F4F6;font-size:13px;color:#6B7280;width:120px'>"
                + label + "</td><td style='padding:8px 12px;border-bottom:1px solid #F3F4F6;font-size:13px;font-weight:600;color:#111827'>"
                + value + "</td></tr>";
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}

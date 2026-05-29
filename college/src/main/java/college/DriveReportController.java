package college;

import helpers.annotations.CollegeRole;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.college.CollegeAccessMiddleware;
import models.body.CollegeLoginRequest;
import models.repos.DriveApplicationRepository;
import models.repos.DriveRepository;
import models.repos.OfferRepository;
import models.services.CsvBuilder;
import models.sql.Drive;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * TPO downloads a CSV report of placement drives.
 *
 * GET /reports/drives?year=2026
 */
@CollegeRole
public enum DriveReportController implements BaseController {

    INSTANCE;

    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("dd-MMM-yyyy");

    @Override
    public void handle(RoutingContext event) {
        CollegeAccessMiddleware.INSTANCE.with(event, new ArrayList<>(), this.getClass())
                .map(this::map)
                .subscribe(
                        csv -> writeCsvResponse(event, "drives-report.csv", (String) csv),
                        error -> ResponseUtils.INSTANCE.handleError(event, error)
                );
    }

    private Object map(CollegeLoginRequest request) {
        Long collegeId = request.getCollege().getId();
        String yearParam = request.getRoutingContext().queryParams().get("year");

        List<Drive> drives;
        if (yearParam != null && !yearParam.isEmpty()) {
            drives = DriveRepository.INSTANCE.byCollegeAndYear(collegeId, Integer.parseInt(yearParam));
        } else {
            drives = DriveRepository.INSTANCE.byCollege(collegeId);
        }

        CsvBuilder csv = new CsvBuilder();
        csv.header("ID", "Title", "Company", "Employment Type", "Status",
                "Academic Year", "CTC Offered", "Stipend", "Location", "Remote",
                "Min CGPA", "Max Backlogs", "Eligible Departments",
                "Registration Deadline", "Drive Date", "Venue",
                "Total Applications", "Selected", "Offers Made");

        for (Drive d : drives) {
            String companyName = d.companyCollege != null && d.companyCollege.company != null
                    ? d.companyCollege.company.name : "";
            int totalApps = DriveApplicationRepository.INSTANCE.countByDrive(d.getId());
            int selected = DriveApplicationRepository.INSTANCE.countByDriveAndStatus(d.getId(),
                    models.enums.ApplicationStatus.SELECTED);
            int offers = OfferRepository.INSTANCE.byDrive(d.getId()).size();

            csv.row(
                    str(d.getId()),
                    str(d.title),
                    companyName,
                    d.employmentType != null ? d.employmentType.name() : "",
                    d.status != null ? d.status.name() : "",
                    str(d.academicYear),
                    d.ctcOffered != null ? d.ctcOffered.toPlainString() : "",
                    d.stipend != null ? d.stipend.toPlainString() : "",
                    str(d.location),
                    str(d.isRemote),
                    d.minCgpa != null ? d.minCgpa.toPlainString() : "",
                    str(d.maxActiveBacklogs),
                    d.eligibleDepartments != null ? String.join("; ", d.eligibleDepartments) : "",
                    d.registrationDeadline != null ? DATE_FMT.format(d.registrationDeadline) : "",
                    d.driveDate != null ? DATE_FMT.format(d.driveDate) : "",
                    str(d.venue),
                    str(totalApps),
                    str(selected),
                    str(offers)
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

package company;

import helpers.annotations.CompanyRole;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.company.CompanyAccessMiddleware;
import models.body.CompanyLoginRequest;
import models.repos.DriveApplicationRepository;
import models.repos.DriveRepository;
import models.services.CsvBuilder;
import models.sql.Drive;
import models.sql.DriveApplication;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Company HR downloads a CSV report of applications for a specific drive.
 *
 * GET /:companyId/reports/drives/:driveId/applications
 */
@CompanyRole
public enum CompanyApplicationReportController implements BaseController {

    INSTANCE;

    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("dd-MMM-yyyy");

    @Override
    public void handle(RoutingContext event) {
        CompanyAccessMiddleware.INSTANCE.with(event, new ArrayList<>(), this.getClass())
                .map(this::map)
                .subscribe(
                        csv -> writeCsvResponse(event, "drive-applications-report.csv", (String) csv),
                        error -> ResponseUtils.INSTANCE.handleError(event, error)
                );
    }

    private Object map(CompanyLoginRequest request) {
        Long companyId = Long.parseLong(request.getRoutingContext().pathParam("companyId"));
        Long driveId = Long.parseLong(request.getRoutingContext().pathParam("driveId"));

        Drive drive = DriveRepository.INSTANCE.byId(driveId);
        if (drive == null) {
            throw new RoutingError("Drive not found");
        }

        // Verify drive belongs to this company
        if (drive.companyCollege == null || drive.companyCollege.company == null
                || !drive.companyCollege.company.getId().equals(companyId)) {
            throw new RoutingError("Drive does not belong to this company");
        }

        List<DriveApplication> applications = DriveApplicationRepository.INSTANCE.byDrive(driveId);

        String collegeName = drive.companyCollege.college != null ? drive.companyCollege.college.name : "";

        CsvBuilder csv = new CsvBuilder();
        csv.header("Application ID", "Student Name", "Email", "Enrollment No",
                "Department", "Passing Year", "CGPA", "Active Backlogs",
                "Status", "Applied At", "College");

        for (DriveApplication a : applications) {
            csv.row(
                    str(a.getId()),
                    a.student != null && a.student.user != null ? str(a.student.user.name) : "",
                    a.student != null && a.student.user != null ? str(a.student.user.email) : "",
                    a.student != null ? str(a.student.enrollmentNumber) : "",
                    a.student != null ? str(a.student.department) : "",
                    a.student != null ? str(a.student.passingYear) : "",
                    a.student != null && a.student.cgpa != null ? a.student.cgpa.toPlainString() : "",
                    a.student != null ? str(a.student.activeBacklogs) : "",
                    a.status != null ? a.status.name() : "",
                    a.getCreatedAt() != null ? DATE_FMT.format(a.getCreatedAt()) : "",
                    collegeName
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

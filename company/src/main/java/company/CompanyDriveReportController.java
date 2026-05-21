package company;

import helpers.annotations.UserAnnotation;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.user.UserAccessMiddleware;
import models.body.UserLoginRequest;
import models.enums.UserType;
import models.repos.DriveApplicationRepository;
import models.repos.DriveRepository;
import models.repos.OfferRepository;
import models.services.CsvBuilder;
import models.sql.Drive;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Company HR downloads a CSV report of all drives across linked colleges.
 *
 * GET /:companyId/reports/drives
 */
@UserAnnotation
public enum CompanyDriveReportController implements BaseController {

    INSTANCE;

    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("dd-MMM-yyyy");

    @Override
    public void handle(RoutingContext event) {
        UserAccessMiddleware.INSTANCE.with(event, new ArrayList<>(), this.getClass())
                .map(this::map)
                .subscribe(
                        csv -> writeCsvResponse(event, "company-drives-report.csv", (String) csv),
                        error -> ResponseUtils.INSTANCE.handleError(event, error)
                );
    }

    private Object map(UserLoginRequest request) {
        UserType userType = request.getUser().getUserType();
        if (!userType.equals(UserType.COMPANY_HR)) {
            throw new RoutingError("Not authorized — company HR only");
        }

        Long companyId = Long.parseLong(request.getRoutingContext().pathParam("companyId"));
        List<Drive> drives = DriveRepository.INSTANCE.byCompany(companyId);

        CsvBuilder csv = new CsvBuilder();
        csv.header("ID", "Title", "College", "Employment Type", "Status",
                "CTC Offered", "Stipend", "Location", "Remote",
                "Registration Deadline", "Drive Date",
                "Total Applications", "Selected", "Offers Made");

        for (Drive d : drives) {
            String collegeName = d.companyCollege != null && d.companyCollege.college != null
                    ? d.companyCollege.college.name : "";
            int totalApps = DriveApplicationRepository.INSTANCE.countByDrive(d.getId());
            int selected = DriveApplicationRepository.INSTANCE.countByDriveAndStatus(d.getId(),
                    models.enums.ApplicationStatus.SELECTED);
            int offers = OfferRepository.INSTANCE.byDrive(d.getId()).size();

            csv.row(
                    str(d.getId()),
                    str(d.title),
                    collegeName,
                    d.employmentType != null ? d.employmentType.name() : "",
                    d.status != null ? d.status.name() : "",
                    d.ctcOffered != null ? d.ctcOffered.toPlainString() : "",
                    d.stipend != null ? d.stipend.toPlainString() : "",
                    str(d.location),
                    str(d.isRemote),
                    d.registrationDeadline != null ? DATE_FMT.format(d.registrationDeadline) : "",
                    d.driveDate != null ? DATE_FMT.format(d.driveDate) : "",
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

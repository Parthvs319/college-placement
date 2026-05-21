package college;

import helpers.annotations.UserAnnotation;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.user.UserAccessMiddleware;
import models.body.UserLoginRequest;
import models.enums.UserType;
import models.services.CsvBuilder;
import models.sql.Offer;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * TPO downloads a CSV report of all offers for the college.
 *
 * GET /reports/offers?status=ACCEPTED&year=2026
 */
@UserAnnotation
public enum OfferReportController implements BaseController {

    INSTANCE;

    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("dd-MMM-yyyy");

    @Override
    public void handle(RoutingContext event) {
        UserAccessMiddleware.INSTANCE.with(event, new ArrayList<>(), this.getClass())
                .map(this::map)
                .subscribe(
                        csv -> writeCsvResponse(event, "offers-report.csv", (String) csv),
                        error -> ResponseUtils.INSTANCE.handleError(event, error)
                );
    }

    private Object map(UserLoginRequest request) {
        UserType userType = request.getUser().getUserType();
        if (!userType.equals(UserType.COLLEGE_ADMIN) && !userType.equals(UserType.TPO) && !userType.equals(UserType.SUPER_ADMIN)) {
            throw new RoutingError("Not authorized to generate reports");
        }

        Long collegeId = request.getUser().college.getId();
        String statusParam = request.getRoutingContext().queryParams().get("status");

        // Query offers for this college's students
        var expr = models.repos.OfferRepository.INSTANCE.where()
                .eq("student.college.id", collegeId);

        if (statusParam != null && !statusParam.isEmpty()) {
            expr.eq("status", models.enums.OfferStatus.valueOf(statusParam.toUpperCase()));
        }

        List<Offer> offers = expr.orderBy("createdAt desc").findList();

        CsvBuilder csv = new CsvBuilder();
        csv.header("ID", "Student Name", "Enrollment No", "Department",
                "Company", "Drive Title", "Designation", "CTC Offered",
                "Location", "Status", "Response Deadline", "Responded At",
                "Offer Letter URL", "Notes", "Created At");

        for (Offer o : offers) {
            String studentName = o.student != null && o.student.user != null ? o.student.user.name : "";
            String enrollment = o.student != null ? str(o.student.enrollmentNumber) : "";
            String department = o.student != null ? str(o.student.department) : "";
            String companyName = o.drive != null && o.drive.companyCollege != null && o.drive.companyCollege.company != null
                    ? o.drive.companyCollege.company.name : "";
            String driveTitle = o.drive != null ? str(o.drive.title) : "";

            csv.row(
                    str(o.getId()),
                    studentName,
                    enrollment,
                    department,
                    companyName,
                    driveTitle,
                    str(o.designation),
                    o.ctcOffered != null ? o.ctcOffered.toPlainString() : "",
                    str(o.location),
                    o.status != null ? o.status.name() : "",
                    o.responseDeadline != null ? DATE_FMT.format(o.responseDeadline) : "",
                    o.respondedAt != null ? DATE_FMT.format(o.respondedAt) : "",
                    str(o.offerLetterUrl),
                    str(o.notes),
                    o.getCreatedAt() != null ? DATE_FMT.format(o.getCreatedAt()) : ""
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

package college.company;

import helpers.annotations.CollegeRole;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.college.CollegeAccessMiddleware;
import models.body.CollegeLoginRequest;
import models.repos.CompanyCollegeRepository;
import models.repos.OfferRepository;
import models.sql.CompanyCollege;
import models.sql.Offer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * GET /college/companies/:companyCollegeId/offers
 *
 * Returns all offers issued by this company to students of the requesting college.
 */
@CollegeRole
public enum GetCompanyOffersController implements BaseController {

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
        Long companyCollegeId;
        try {
            companyCollegeId = Long.parseLong(event.pathParam("companyCollegeId"));
        } catch (NumberFormatException e) {
            throw new RoutingError(400, "Invalid companyCollegeId");
        }

        // Verify this companyCollege belongs to the requesting college
        CompanyCollege cc = CompanyCollegeRepository.INSTANCE.byId(companyCollegeId);
        if (cc == null || !cc.getCollege().getId().equals(request.getCollege().getId())) {
            throw new RoutingError(403, "Not authorized to view these offers");
        }

        List<Offer> offers = OfferRepository.INSTANCE.byCompanyCollege(companyCollegeId);

        return offers.stream().map(o -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",               o.getId());
            m.put("status",           o.getStatus() != null ? o.getStatus().name() : null);
            m.put("ctcOffered",       o.ctcOffered);
            m.put("designation",      o.designation);
            m.put("location",         o.location);
            m.put("offerLetterUrl",   o.offerLetterUrl);
            m.put("responseDeadline", o.responseDeadline);
            m.put("respondedAt",      o.respondedAt);
            m.put("createdAt",        o.getCreatedAt());

            // Student
            if (o.student != null) {
                m.put("studentId",         o.student.getId());
                m.put("studentName",       o.student.getUser() != null ? o.student.getUser().getName() : null);
                m.put("studentEmail",      o.student.getUser() != null ? o.student.getUser().getEmail() : null);
                m.put("enrollmentNumber",  o.student.getEnrollmentNumber());
                m.put("department",        o.student.getDepartment());
            }

            // Drive
            if (o.drive != null) {
                m.put("driveId",           o.drive.getId());
                m.put("driveTitle",        o.drive.getTitle());
                m.put("driveCode",         o.drive.getCode());
                m.put("employmentType",    o.drive.getEmploymentType() != null ? o.drive.getEmploymentType().name() : null);
            }

            return m;
        }).collect(Collectors.toList());
    }
}

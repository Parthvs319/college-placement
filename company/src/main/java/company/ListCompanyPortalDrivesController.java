package company;

import helpers.annotations.CompanyRole;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.company.CompanyAccessMiddleware;
import models.body.CompanyLoginRequest;
import models.repos.DriveRepository;
import models.sql.Drive;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * GET /company/drives
 * Lists all drives across all colleges linked to this company.
 */
@CompanyRole
public enum ListCompanyPortalDrivesController implements BaseController {

    INSTANCE;

    @Override
    public void handle(RoutingContext event) {
        CompanyAccessMiddleware.INSTANCE.with(event, new ArrayList<>(), this.getClass())
                .map(this::map)
                .subscribe(
                        o -> ResponseUtils.INSTANCE.writeJsonResponse(event, o),
                        error -> ResponseUtils.INSTANCE.handleError(event, error)
                );
    }

    private Object map(CompanyLoginRequest request) {
        if (request.getCompany() == null) throw new RoutingError(404, "No company linked to your account");

        Long companyId = request.getCompany().getId();
        List<Drive> drives = DriveRepository.INSTANCE.byCompany(companyId);

        List<Map<String, Object>> list = new ArrayList<>();
        for (Drive d : drives) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",                   d.getId());
            m.put("driveCode",            d.driveCode);
            m.put("title",                d.title);
            m.put("status",               d.status);
            m.put("employmentType",       d.employmentType);
            m.put("jobDescription",       d.jobDescription);
            m.put("ctcOffered",           d.ctcOffered);
            m.put("stipend",              d.stipend);
            m.put("location",             d.location);
            m.put("isRemote",             d.isRemote);
            m.put("minCgpa",              d.minCgpa);
            m.put("maxActiveBacklogs",    d.maxActiveBacklogs);
            m.put("registrationDeadline", d.registrationDeadline);
            m.put("driveDate",            d.driveDate);
            m.put("academicYear",         d.academicYear);
            if (d.companyCollege != null && d.companyCollege.college != null) {
                Map<String, Object> college = new LinkedHashMap<>();
                college.put("id",   d.companyCollege.college.getId());
                college.put("name", d.companyCollege.college.getName());
                college.put("code", d.companyCollege.college.getCode());
                m.put("college", college);
            }
            list.add(m);
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("drives", list);
        res.put("total",  list.size());
        return res;
    }
}

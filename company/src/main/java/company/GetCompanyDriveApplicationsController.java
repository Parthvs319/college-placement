package company;

import helpers.annotations.CompanyRole;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.company.CompanyAccessMiddleware;
import models.body.CompanyLoginRequest;
import models.repos.DriveApplicationRepository;
import models.repos.DriveRepository;
import models.sql.Drive;
import models.sql.DriveApplication;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * GET /company/drives/:driveId/applications
 * Lists all applicants for a drive that belongs to this company.
 */
@CompanyRole
public enum GetCompanyDriveApplicationsController implements BaseController {

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

        String driveIdStr = request.getRoutingContext().pathParam("driveId");
        Long   driveId    = Long.parseLong(driveIdStr);

        Drive drive = DriveRepository.INSTANCE.byId(driveId);
        if (drive == null) throw new RoutingError(404, "Drive not found");

        // Ensure this drive belongs to the requesting company
        if (drive.companyCollege == null
                || !drive.companyCollege.company.getId().equals(request.getCompany().getId())) {
            throw new RoutingError(403, "Drive does not belong to your company");
        }

        List<DriveApplication> apps = DriveApplicationRepository.INSTANCE.byDrive(driveId);

        List<Map<String, Object>> list = new ArrayList<>();
        for (DriveApplication a : apps) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",          a.getId());
            m.put("status",      a.status);
            m.put("appliedAt",   a.getCreatedAt());
            if (a.student != null) {
                Map<String, Object> s = new LinkedHashMap<>();
                s.put("id",               a.student.getId());
                s.put("name",             a.student.getName());
                s.put("email",            a.student.getEmail());
                s.put("enrollmentNumber", a.student.enrollmentNumber);
                s.put("department",       a.student.department);
                s.put("cgpa",             a.student.cgpa);
                s.put("passingYear",      a.student.passingYear);
                s.put("activeBacklogs",   a.student.activeBacklogs);
                s.put("mobile",           a.student.mobile);
                s.put("linkedinUrl",      a.student.linkedinUrl);
                s.put("githubUrl",        a.student.githubUrl);
                m.put("student", s);
            }
            list.add(m);
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("driveId",      driveId);
        res.put("driveTitle",   drive.title);
        res.put("applications", list);
        res.put("total",        list.size());
        return res;
    }
}

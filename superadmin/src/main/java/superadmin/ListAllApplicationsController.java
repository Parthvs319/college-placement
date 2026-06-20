package superadmin;

import helpers.annotations.SuperAdminRole;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.superadmin.SuperAdminAccessMiddleware;
import models.repos.DriveApplicationRepository;
import models.sql.DriveApplication;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@SuperAdminRole
public enum ListAllApplicationsController implements BaseController {

    INSTANCE;

    @Override
    public void handle(RoutingContext event) {
        SuperAdminAccessMiddleware.INSTANCE.with(event, new ArrayList<>(), this.getClass())
                .map(req -> {
                    String driveIdParam = event.request().getParam("driveId");
                    String studentIdParam = event.request().getParam("studentId");

                    List<DriveApplication> apps;
                    if (driveIdParam != null && !driveIdParam.isEmpty()) {
                        apps = DriveApplicationRepository.INSTANCE.byDrive(Long.parseLong(driveIdParam));
                    } else if (studentIdParam != null && !studentIdParam.isEmpty()) {
                        apps = DriveApplicationRepository.INSTANCE.byStudent(Long.parseLong(studentIdParam));
                    } else {
                        apps = DriveApplicationRepository.INSTANCE.where()
                                .orderBy("createdAt desc")
                                .setMaxRows(500)
                                .findList();
                    }

                    return apps.stream().map(a -> {
                        Map<String, Object> m = new HashMap<>();
                        m.put("id", a.getId());
                        m.put("status", a.getStatus() != null ? a.getStatus().name() : null);
                        m.put("appliedAt", a.getCreatedAt() != null ? a.getCreatedAt().toString() : null);

                        if (a.getStudent() != null) {
                            m.put("studentId", a.getStudent().getId());
                            m.put("studentName", a.getStudent().getUser() != null ? a.getStudent().getUser().getName() : null);
                            m.put("collegeName", a.getStudent().getCollege() != null ? a.getStudent().getCollege().getName() : null);
                        }

                        if (a.getDrive() != null) {
                            m.put("driveId", a.getDrive().getId());
                            m.put("driveTitle", a.getDrive().getTitle());
                            if (a.getDrive().getCompanyCollege() != null && a.getDrive().getCompanyCollege().getCompany() != null) {
                                m.put("companyName", a.getDrive().getCompanyCollege().getCompany().getName());
                            }
                        }
                        return m;
                    }).collect(Collectors.toList());
                })
                .subscribe(
                        o -> ResponseUtils.INSTANCE.writeJsonResponse(event, o),
                        error -> ResponseUtils.INSTANCE.handleError(event, error)
                );
    }
}

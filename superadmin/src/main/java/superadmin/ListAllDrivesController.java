package superadmin;

import helpers.annotations.SuperAdminRole;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.superadmin.SuperAdminAccessMiddleware;
import models.repos.DriveApplicationRepository;
import models.repos.DriveRepository;
import models.repos.OfferRepository;
import models.sql.Drive;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@SuperAdminRole
public enum ListAllDrivesController implements BaseController {

    INSTANCE;

    @Override
    public void handle(RoutingContext event) {
        SuperAdminAccessMiddleware.INSTANCE.with(event, new ArrayList<>(), this.getClass())
                .map(req -> {
                    String collegeIdParam = event.request().getParam("collegeId");
                    String statusParam = event.request().getParam("status");

                    var query = DriveRepository.INSTANCE.where();

                    if (collegeIdParam != null && !collegeIdParam.isEmpty()) {
                        query.eq("companyCollege.college.id", Long.parseLong(collegeIdParam));
                    }
                    if (statusParam != null && !statusParam.isEmpty()) {
                        query.eq("status", statusParam);
                    }

                    List<Drive> drives = query.orderBy("driveDate desc").findList();

                    return drives.stream().map(d -> {
                        SuperAdminDtos.DriveOverview o = new SuperAdminDtos.DriveOverview();
                        o.setId(d.getId());
                        o.setTitle(d.title);
                        o.setStatus(d.status != null ? d.status.name() : null);
                        o.setEmploymentType(d.employmentType != null ? d.employmentType.name() : null);
                        o.setCtcOffered(d.ctcOffered);
                        o.setDriveDate(d.driveDate != null ? d.driveDate.toString() : null);

                        if (d.companyCollege != null) {
                            if (d.companyCollege.getCompany() != null) {
                                o.setCompanyName(d.companyCollege.getCompany().name);
                            }
                            if (d.companyCollege.getCollege() != null) {
                                o.setCollegeName(d.companyCollege.getCollege().name);
                            }
                        }

                        o.setApplicationCount(DriveApplicationRepository.INSTANCE.countByDrive(d.getId()));
                        o.setOfferCount(OfferRepository.INSTANCE.byDrive(d.getId()).size());
                        return o;
                    }).collect(Collectors.toList());
                })
                .subscribe(
                        o -> ResponseUtils.INSTANCE.writeJsonResponse(event, o),
                        error -> ResponseUtils.INSTANCE.handleError(event, error)
                );
    }
}

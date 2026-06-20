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
                    String yearParam = event.request().getParam("academicYear");

                    var query = DriveRepository.INSTANCE.where();

                    if (collegeIdParam != null && !collegeIdParam.isEmpty()) {
                        query.eq("companyCollege.college.id", Long.parseLong(collegeIdParam));
                    }
                    if (statusParam != null && !statusParam.isEmpty()) {
                        query.eq("status", statusParam);
                    }
                    if (yearParam != null && !yearParam.isEmpty()) {
                        query.eq("academicYear", Integer.parseInt(yearParam));
                    }

                    List<Drive> drives = query.orderBy("driveDate desc").findList();

                    return drives.stream().map(d -> {
                        SuperAdminDtos.DriveOverview o = new SuperAdminDtos.DriveOverview();
                        o.setId(d.getId());
                        o.setTitle(d.getTitle());
                        o.setStatus(d.getStatus() != null ? d.getStatus().name() : null);
                        o.setEmploymentType(d.getEmploymentType() != null ? d.getEmploymentType().name() : null);
                        o.setCtcOffered(d.getCtcOffered());
                        o.setDriveDate(d.getDriveDate() != null ? d.getDriveDate().toString() : null);

                        if (d.getCompanyCollege() != null) {
                            if (d.getCompanyCollege().getCompany() != null) {
                                o.setCompanyName(d.getCompanyCollege().getCompany().getName());
                            }
                            if (d.getCompanyCollege().getCollege() != null) {
                                o.setCollegeName(d.getCompanyCollege().getCollege().getName());
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

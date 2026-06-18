package superadmin;

import helpers.annotations.SuperAdminRole;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.superadmin.SuperAdminAccessMiddleware;
import models.repos.OfferRepository;
import models.sql.Offer;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@SuperAdminRole
public enum ListAllOffersController implements BaseController {

    INSTANCE;

    @Override
    public void handle(RoutingContext event) {
        SuperAdminAccessMiddleware.INSTANCE.with(event, new ArrayList<>(), this.getClass())
                .map(req -> {
                    String collegeIdParam = event.request().getParam("collegeId");
                    String yearParam = event.request().getParam("academicYear");

                    var query = OfferRepository.INSTANCE.where();
                    if (collegeIdParam != null && !collegeIdParam.isEmpty()) {
                        query.eq("drive.companyCollege.college.id", Long.parseLong(collegeIdParam));
                    }
                    if (yearParam != null && !yearParam.isEmpty()) {
                        query.eq("drive.academicYear", Integer.parseInt(yearParam));
                    }

                    List<Offer> offers = query.orderBy("createdAt desc").findList();

                    return offers.stream().map(o -> {
                        SuperAdminDtos.OfferOverview ov = new SuperAdminDtos.OfferOverview();
                        ov.setId(o.getId());
                        ov.setStatus(o.getStatus() != null ? o.getStatus().name() : null);
                        ov.setCreatedAt(o.getCreatedAt() != null ? o.getCreatedAt().toString() : null);

                        if (o.getStudent() != null) {
                            ov.setStudentName(o.getStudent().user != null ? o.getStudent().user.name : null);
                            ov.setStudentEmail(o.getStudent().user != null ? o.getStudent().user.email : null);
                            if (o.getStudent().college != null) {
                                ov.setCollegeName(o.getStudent().college.name);
                            }
                        }

                        if (o.getDrive() != null) {
                            ov.setDriveName(o.getDrive().title);
                            ov.setCtc(o.getDrive().ctcOffered);
                            if (o.getDrive().companyCollege != null && o.getDrive().companyCollege.getCompany() != null) {
                                ov.setCompanyName(o.getDrive().companyCollege.getCompany().name);
                            }
                        }

                        return ov;
                    }).collect(Collectors.toList());
                })
                .subscribe(
                        o -> ResponseUtils.INSTANCE.writeJsonResponse(event, o),
                        error -> ResponseUtils.INSTANCE.handleError(event, error)
                );
    }
}

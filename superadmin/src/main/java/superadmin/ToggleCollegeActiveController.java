package superadmin;

import helpers.annotations.SuperAdminRole;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.superadmin.SuperAdminAccessMiddleware;
import models.repos.CollegeRepository;
import models.sql.College;

import java.util.ArrayList;
import java.util.Map;

@SuperAdminRole
public enum ToggleCollegeActiveController implements BaseController {

    INSTANCE;

    @Override
    public void handle(RoutingContext event) {
        SuperAdminAccessMiddleware.INSTANCE.with(event, new ArrayList<>(), this.getClass())
                .map(req -> {
                    Long collegeId = Long.parseLong(event.pathParam("collegeId"));
                    College college = CollegeRepository.INSTANCE.byId(collegeId);
                    if (college == null) throw new RoutingError(404, "College not found");
                    college.setActive(!college.isActive());
                    System.out.println("yess : " + college.isActive());
                    college.update();
                    System.out.println("yess : " + college.isActive());
                    return Map.of(
                            "message", college.isActive() ? "College activated" : "College deactivated",
                            "collegeId", collegeId,
                            "active", college.isActive()
                    );
                })
                .subscribe(
                        o -> ResponseUtils.INSTANCE.writeJsonResponse(event, o),
                        error -> ResponseUtils.INSTANCE.handleError(event, error)
                );
    }
}

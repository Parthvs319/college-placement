package college.company;

import helpers.annotations.CollegeRole;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.ebean.DB;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.college.CollegeAccessMiddleware;
import models.repos.CompanyCollegeRepository;
import models.sql.CompanyCollege;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * POST /college/companies/:companyCollegeId/toggle-active
 * Toggles the active flag on a CompanyCollege link.
 * Only the college that owns the link can toggle it.
 */
@CollegeRole
public enum ToggleCompanyActiveController implements BaseController {

    INSTANCE;

    @Override
    public void handle(RoutingContext event) {
        CollegeAccessMiddleware.INSTANCE.with(event, new ArrayList<>(), this.getClass())
                .map(req -> {
                    Long ccId = Long.parseLong(event.pathParam("companyCollegeId"));
                    CompanyCollege cc = CompanyCollegeRepository.INSTANCE.byId(ccId);
                    if (cc == null) throw new RoutingError(404, "Company link not found");

                    // Ensure this link belongs to the requesting college
                    Long collegeId = req.getCollege().getId();
                    if (cc.getCollege() == null || !cc.getCollege().getId().equals(collegeId)) {
                        throw new RoutingError(403, "Not authorized to modify this company link");
                    }

                    // Toggle
                    cc.setActive(!cc.isActive());
                    DB.save(cc);

                    Map<String, Object> result = new HashMap<>();
                    result.put("id", cc.getId());
                    result.put("active", cc.isActive());
                    result.put("message", cc.isActive() ? "Company activated" : "Company deactivated");
                    return result;
                })
                .subscribe(
                        o -> ResponseUtils.INSTANCE.writeJsonResponse(event, o),
                        error -> ResponseUtils.INSTANCE.handleError(event, error)
                );
    }
}

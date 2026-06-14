package college;

import helpers.annotations.CollegeRole;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.college.CollegeAccessMiddleware;
import models.body.CollegeLoginRequest;
import models.json.CollegeDtos;
import models.repos.CollegeRepository;
import models.sql.College;

import java.util.ArrayList;

@CollegeRole
public enum UpdateCollegeController implements BaseController {

    INSTANCE;

    @Override
    public void handle(RoutingContext event) {
        CollegeAccessMiddleware.INSTANCE.with(event, new ArrayList<>(), this.getClass())
                .map(this::map)
                .subscribe(
                        o -> ResponseUtils.INSTANCE.writeJsonResponse(event, o),
                        error -> ResponseUtils.INSTANCE.handleError(event, error)
                );
    }

    private Object map(CollegeLoginRequest request) {
        College college = CollegeRepository.INSTANCE.byId(request.getCollege().getId());
        if (college == null) {
            throw new RoutingError("College not found");
        }
        if (request.getRequest().isPresent("name")) college.name = request.getRequest().get("name");
        if (request.getRequest().isPresent("address")) college.address = request.getRequest().get("address");
        if (request.getRequest().isPresent("cityId")) college.cityId = Long.parseLong(request.getRequest().get("cityId"));
        if (request.getRequest().isPresent("stateId")) college.stateId = Long.parseLong(request.getRequest().get("stateId"));
        if (request.getRequest().isPresent("website")) college.website = request.getRequest().get("website");
        if (request.getRequest().isPresent("logoUrl")) college.logoUrl = request.getRequest().get("logoUrl");
        if (request.getRequest().isPresent("contactEmail")) college.contactEmail = request.getRequest().get("contactEmail");
        if (request.getRequest().isPresent("contactPhone")) college.contactPhone = request.getRequest().get("contactPhone");
        college.update();
        return CollegeDtos.toCollegeDto(college);
    }
}

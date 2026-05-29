package college;

import helpers.annotations.CollegeRole;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.college.CollegeAccessMiddleware;
import models.body.CollegeLoginRequest;
import models.json.CollegeDtos;
import models.repos.PlacementPolicyRepository;
import models.sql.PlacementPolicy;

import java.util.ArrayList;

@CollegeRole
public enum GetPolicyController implements BaseController {

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
        Long collegeId = request.getCollege().getId();

        // Return latest policy or by year if query param provided
        String yearParam = request.getRoutingContext().queryParam("year").isEmpty()
                ? null : request.getRoutingContext().queryParam("year").get(0);

        PlacementPolicy policy;
        if (yearParam != null) {
            policy = PlacementPolicyRepository.INSTANCE.byCollegeAndYear(collegeId, Integer.parseInt(yearParam));
        } else {
            policy = PlacementPolicyRepository.INSTANCE.latestByCollege(collegeId);
        }

        if (policy == null) {
            throw new RoutingError("No placement policy found");
        }

        return CollegeDtos.toPolicyDto(policy);
    }
}

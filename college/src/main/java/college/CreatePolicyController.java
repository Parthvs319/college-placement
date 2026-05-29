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
import models.repos.PlacementPolicyRepository;
import models.sql.College;
import models.sql.PlacementPolicy;

import java.math.BigDecimal;
import java.util.ArrayList;

@CollegeRole
public enum CreatePolicyController implements BaseController {

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
        College college = CollegeRepository.INSTANCE.byId(collegeId);
        if (college == null) {
            throw new RoutingError("College not found");
        }
        String yearStr = request.getRequest().get("academicYear");
        if (yearStr == null) {
            throw new RoutingError("academicYear is required");
        }
        int academicYear = Integer.parseInt(yearStr);

        // Check if policy already exists for this year
        PlacementPolicy existing = PlacementPolicyRepository.INSTANCE.byCollegeAndYear(collegeId, academicYear);
        if (existing != null) {
            throw new RoutingError("Policy for academic year " + academicYear + " already exists. Use PUT to update.");
        }

        PlacementPolicy policy = new PlacementPolicy();
        policy.college = college;
        policy.academicYear = academicYear;

        if (request.getRequest().isPresent("dreamCtcThreshold")) {
            policy.dreamCtcThreshold = new BigDecimal(String.valueOf(request.getRequest().get("dreamCtcThreshold")));
        }
        if (request.getRequest().isPresent("maxSimultaneousOffers")) {
            policy.maxSimultaneousOffers = Integer.parseInt(request.getRequest().get("maxSimultaneousOffers"));
        }
        if (request.getRequest().isPresent("blockAfterFirstAccept")) {
            policy.blockAfterFirstAccept = Boolean.parseBoolean(request.getRequest().get("blockAfterFirstAccept"));
        }
        if (request.getRequest().isPresent("autoFilterEnabled")) {
            policy.autoFilterEnabled = Boolean.parseBoolean(request.getRequest().get("autoFilterEnabled"));
        }
        if (request.getRequest().isPresent("offerExpiryDays")) {
            policy.offerExpiryDays = Integer.parseInt(request.getRequest().get("offerExpiryDays"));
        }
        if (request.getRequest().isPresent("description")) {
            policy.description = request.getRequest().get("description");
        }

        policy.save();
        return CollegeDtos.toPolicyDto(policy);
    }
}

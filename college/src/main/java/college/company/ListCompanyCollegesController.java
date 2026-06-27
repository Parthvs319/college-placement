package college.company;

import helpers.annotations.CollegeRole;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.college.CollegeAccessMiddleware;
import models.body.CollegeLoginRequest;
import models.json.CollegeDtos;
import models.repos.CompanyCollegeRepository;

import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * TPO lists all companies linked to their college.
 */
@CollegeRole
public enum ListCompanyCollegesController implements BaseController {

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
        return CompanyCollegeRepository.INSTANCE.byCollege(collegeId).stream().map(CollegeDtos::toCompanyCollegeDto).collect(Collectors.toList());
    }
}

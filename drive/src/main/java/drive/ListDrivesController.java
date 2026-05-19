package drive;

import helpers.annotations.UserAnnotation;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.user.UserAccessMiddleware;
import models.body.UserLoginRequest;
import models.repos.DriveRepository;

import java.util.ArrayList;
import java.util.List;

@UserAnnotation
public enum ListDrivesController implements BaseController {

    INSTANCE;

    @Override
    public void handle(RoutingContext event) {
        UserAccessMiddleware.INSTANCE.with(event, new ArrayList<>(), this.getClass())
                .map(this::map)
                .subscribe(
                        o -> ResponseUtils.INSTANCE.writeJsonResponse(event, o),
                        error -> ResponseUtils.INSTANCE.handleError(event, error)
                );
    }

    private Object map(UserLoginRequest request) {
        String collegeIdParam = request.getRoutingContext().pathParam("collegeId");
        Long collegeId = Long.parseLong(collegeIdParam);

        // Optional year filter
        List<String> yearParam = request.getRoutingContext().queryParam("year");
        if (!yearParam.isEmpty()) {
            int year = Integer.parseInt(yearParam.get(0));
            return DriveRepository.INSTANCE.byCollegeAndYear(collegeId, year);
        }

        return DriveRepository.INSTANCE.byCollege(collegeId);
    }
}

package college;

import helpers.annotations.UserAnnotation;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.user.UserAccessMiddleware;
import models.body.UserLoginRequest;
import models.repos.CollegeRepository;
import models.sql.College;

import java.util.ArrayList;

@UserAnnotation
public enum GetCollegeController implements BaseController {

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
        String idParam = request.getRoutingContext().pathParam("id");
        if (idParam == null) {
            throw new RoutingError("College ID is required");
        }

        College college = CollegeRepository.INSTANCE.byId(Long.parseLong(idParam));
        if (college == null) {
            throw new RoutingError("College not found");
        }

        return college;
    }
}

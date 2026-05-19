package college;

import helpers.annotations.UserAnnotation;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.user.UserAccessMiddleware;
import models.body.UserLoginRequest;
import models.enums.UserType;
import models.repos.CollegeRepository;
import models.sql.College;

import java.util.ArrayList;

@UserAnnotation
public enum CreateCollegeController implements BaseController {

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
        if (!request.getUser().getUserType().equals(UserType.SUPER_ADMIN)) {
            throw new RoutingError("Only super admins can create colleges");
        }

        var body = request.getRequest();
        String name = body.get("name");
        String code = body.get("code");

        if (name == null || code == null) {
            throw new RoutingError("name and code are required");
        }

        // Check unique code
        if (CollegeRepository.INSTANCE.byCode(code) != null) {
            throw new RoutingError("College with code " + code + " already exists");
        }

        College college = new College();
        college.name = name;
        college.code = code.toUpperCase();
        college.address = body.get("address");
        college.city = body.get("city");
        college.state = body.get("state");
        college.website = body.get("website");
        college.logoUrl = body.get("logoUrl");
        college.contactEmail = body.get("contactEmail");
        college.contactPhone = body.get("contactPhone");
        college.save();

        return college;
    }
}

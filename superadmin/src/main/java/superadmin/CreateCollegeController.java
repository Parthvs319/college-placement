package superadmin;

import helpers.annotations.SuperAdminRole;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.superadmin.SuperAdminAccessMiddleware;
import models.body.SuperAdminLoginRequest;
import models.json.CollegeDtos;
import models.repos.CollegeRepository;
import models.sql.College;

import java.util.ArrayList;

@SuperAdminRole(request = {
        "name:string@required",
        "code:string@required",
        "city:string",
        "state:string",
        "university:string",
        "website:string",
        "contactEmail:string",
        "contactPhone:string"
})
public enum CreateCollegeController implements BaseController {

    INSTANCE;

    @Override
    public void handle(RoutingContext event) {
        SuperAdminAccessMiddleware.INSTANCE.with(event, new ArrayList<>(), this.getClass())
                .map(this::map)
                .subscribe(
                        o -> ResponseUtils.INSTANCE.writeJsonResponse(event, o),
                        error -> ResponseUtils.INSTANCE.handleError(event, error)
                );
    }

    private Object map(SuperAdminLoginRequest request) {
        var body = request.getRequest();
        String name = body.get("name");
        String code = body.get("code");

        if (name == null || name.isBlank()) {
            throw new RoutingError("College name is required");
        }
        if (code == null || code.isBlank()) {
            throw new RoutingError("College code is required");
        }
        if (CollegeRepository.INSTANCE.byCode(code.toUpperCase()) != null) {
            throw new RoutingError("College with code " + code.toUpperCase() + " already exists");
        }

        College college = new College();
        college.name = name;
        college.code = code.toUpperCase();
        college.city = body.get("city");
        college.state = body.get("state");
        college.university = body.get("university");
        college.website = body.get("website");
        college.contactEmail = body.get("contactEmail");
        college.contactPhone = body.get("contactPhone");
        college.verified = false;
        college.active = false;
        college.save();

        return CollegeDtos.toCollegeDto(college);
    }
}

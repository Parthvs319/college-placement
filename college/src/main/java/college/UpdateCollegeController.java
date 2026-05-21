package college;

import helpers.annotations.UserAnnotation;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.user.UserAccessMiddleware;
import models.body.UserLoginRequest;
import models.enums.UserType;
import models.json.CollegeDtos;
import models.repos.CollegeRepository;
import models.sql.College;

import java.util.ArrayList;

@UserAnnotation
public enum UpdateCollegeController implements BaseController {

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
        UserType userType = request.getUser().getUserType();
        if (!userType.equals(UserType.COLLEGE_ADMIN) && !userType.equals(UserType.TPO)) {
            throw new RoutingError("Not authorized to update college");
        }

        if (request.getUser().college == null) {
            throw new RoutingError("No college linked to your account");
        }

        College college = CollegeRepository.INSTANCE.byId(request.getUser().college.getId());
        if (college == null) {
            throw new RoutingError("College not found");
        }

        if (request.getRequest().isPresent("name")) college.name = request.getRequest().get("name");
        if (request.getRequest().isPresent("address")) college.address = request.getRequest().get("address");
        if (request.getRequest().isPresent("city")) college.city = request.getRequest().get("city");
        if (request.getRequest().isPresent("state")) college.state = request.getRequest().get("state");
        if (request.getRequest().isPresent("website")) college.website = request.getRequest().get("website");
        if (request.getRequest().isPresent("logoUrl")) college.logoUrl = request.getRequest().get("logoUrl");
        if (request.getRequest().isPresent("contactEmail")) college.contactEmail = request.getRequest().get("contactEmail");
        if (request.getRequest().isPresent("contactPhone")) college.contactPhone = request.getRequest().get("contactPhone");
        college.update();

        return CollegeDtos.toCollegeDto(college);
    }
}

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
import models.repos.CompanyCollegeRepository;

import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * TPO lists all companies linked to their college.
 */
@UserAnnotation
public enum ListCompanyCollegesController implements BaseController {

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
        if (!userType.equals(UserType.COLLEGE_ADMIN) && !userType.equals(UserType.TPO) && !userType.equals(UserType.SUPER_ADMIN)) {
            throw new RoutingError("Not authorized");
        }

        Long collegeId = request.getUser().college.getId();
        return CompanyCollegeRepository.INSTANCE.byCollege(collegeId).stream().map(CollegeDtos::toCompanyCollegeDto).collect(Collectors.toList());
    }
}

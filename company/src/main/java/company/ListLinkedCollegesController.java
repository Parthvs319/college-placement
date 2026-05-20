package company;

import helpers.annotations.UserAnnotation;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.user.UserAccessMiddleware;
import models.body.UserLoginRequest;
import models.enums.UserType;
import models.repos.CompanyCollegeRepository;

import java.util.ArrayList;

/**
 * Company HR views all colleges linked to their company.
 * (Company-side view — different from college portal's view)
 */
@UserAnnotation
public enum ListLinkedCollegesController implements BaseController {

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
        if (!userType.equals(UserType.COMPANY_HR)) {
            throw new RoutingError("Not authorized — company HR only");
        }

        Long companyId = Long.parseLong(request.getRoutingContext().pathParam("companyId"));
        return CompanyCollegeRepository.INSTANCE.byCompany(companyId);
    }
}

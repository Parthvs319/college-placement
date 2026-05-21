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
import models.repos.OfferRepository;

import java.util.ArrayList;
import java.util.stream.Collectors;

@UserAnnotation
public enum ListDriveOffersController implements BaseController {

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
            throw new RoutingError("Not authorized to view offers");
        }

        String driveIdParam = request.getRoutingContext().pathParam("driveId");
        return OfferRepository.INSTANCE.byDrive(Long.parseLong(driveIdParam)).stream().map(CollegeDtos::toOfferDto).collect(Collectors.toList());
    }
}

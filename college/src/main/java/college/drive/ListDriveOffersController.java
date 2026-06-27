package college.drive;

import helpers.annotations.CollegeRole;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.college.CollegeAccessMiddleware;
import models.body.CollegeLoginRequest;
import models.json.CollegeDtos;
import models.repos.OfferRepository;

import java.util.ArrayList;
import java.util.stream.Collectors;

@CollegeRole
public enum ListDriveOffersController implements BaseController {

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
        String driveIdParam = request.getRoutingContext().pathParam("driveId");
        return OfferRepository.INSTANCE.byDrive(Long.parseLong(driveIdParam)).stream().map(CollegeDtos::toOfferDto).collect(Collectors.toList());
    }
}

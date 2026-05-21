package student;

import helpers.annotations.UserAnnotation;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.user.UserAccessMiddleware;
import models.body.UserLoginRequest;
import models.repos.DriveRepository;
import models.sql.Drive;

import java.util.ArrayList;

/**
 * Student views details of a specific drive.
 */
@UserAnnotation
public enum GetDriveDetailController implements BaseController {

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
        String driveIdParam = request.getRoutingContext().pathParam("driveId");
        Drive drive = DriveRepository.INSTANCE.byId(Long.parseLong(driveIdParam));
        if (drive == null) {
            throw new RoutingError("Drive not found");
        }
        return StudentDtos.toDriveDetailDto(drive);
    }
}

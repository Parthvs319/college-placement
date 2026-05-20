package college;

import helpers.annotations.UserAnnotation;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.user.UserAccessMiddleware;
import models.body.UserLoginRequest;
import models.enums.ApplicationStatus;
import models.enums.UserType;
import models.repos.DriveApplicationRepository;

import java.util.ArrayList;
import java.util.List;

@UserAnnotation
public enum ListDriveApplicationsController implements BaseController {

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
            throw new RoutingError("Not authorized to view applications");
        }

        String driveIdParam = request.getRoutingContext().pathParam("driveId");
        Long driveId = Long.parseLong(driveIdParam);

        List<String> statusParam = request.getRoutingContext().queryParam("status");
        if (!statusParam.isEmpty()) {
            ApplicationStatus status = ApplicationStatus.valueOf(statusParam.get(0));
            return DriveApplicationRepository.INSTANCE.byDriveAndStatus(driveId, status);
        }

        return DriveApplicationRepository.INSTANCE.byDrive(driveId);
    }
}

package drive;

import helpers.annotations.CollegeRole;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.college.CollegeAccessMiddleware;
import models.body.CollegeLoginRequest;
import models.enums.ApplicationStatus;
import models.repos.DriveApplicationRepository;

import java.util.ArrayList;
import java.util.List;

@CollegeRole
public enum ListDriveApplicationsController implements BaseController {

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
        Long driveId = Long.parseLong(driveIdParam);

        // Optional status filter
        List<String> statusParam = request.getRoutingContext().queryParam("status");
        if (!statusParam.isEmpty()) {
            ApplicationStatus status = ApplicationStatus.valueOf(statusParam.get(0));
            return DriveApplicationRepository.INSTANCE.byDriveAndStatus(driveId, status);
        }

        return DriveApplicationRepository.INSTANCE.byDrive(driveId);
    }
}

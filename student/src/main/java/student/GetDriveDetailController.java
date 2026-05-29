package student;

import helpers.annotations.StudentRole;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.student.StudentAccessMiddleware;
import models.body.StudentLoginRequest;
import models.repos.DriveRepository;
import models.sql.Drive;

import java.util.ArrayList;

/**
 * Student views details of a specific drive.
 */
@StudentRole
public enum GetDriveDetailController implements BaseController {

    INSTANCE;

    @Override
    public void handle(RoutingContext event) {
        StudentAccessMiddleware.INSTANCE.with(event, new ArrayList<>(), this.getClass())
                .map(this::map)
                .subscribe(
                        o -> ResponseUtils.INSTANCE.writeJsonResponse(event, o),
                        error -> ResponseUtils.INSTANCE.handleError(event, error)
                );
    }

    private Object map(StudentLoginRequest request) {
        String driveIdParam = request.getRoutingContext().pathParam("driveId");
        Drive drive = DriveRepository.INSTANCE.byId(Long.parseLong(driveIdParam));
        if (drive == null) {
            throw new RoutingError("Drive not found");
        }
        return StudentDtos.toDriveDetailDto(drive);
    }
}

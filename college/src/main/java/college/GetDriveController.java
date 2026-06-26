package college;

import helpers.annotations.CollegeRole;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.college.CollegeAccessMiddleware;
import models.body.CollegeLoginRequest;
import models.json.CollegeDtos;
import models.repos.DriveApplicationRepository;
import models.repos.DriveRepository;
import models.repos.DriveRoundRepository;
import models.sql.Drive;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@CollegeRole
public enum GetDriveController implements BaseController {

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
        String idParam = request.getRoutingContext().pathParam("driveId");
        Drive drive = DriveRepository.INSTANCE.byId(Long.parseLong(idParam));
        if (drive == null) {
            throw new RoutingError("Drive not found");
        }
        // Verify drive belongs to this college
        if (drive.getCompanyCollege() == null ||
                !drive.getCompanyCollege().getCollege().getId().equals(request.getCollege().getId())) {
            throw new RoutingError("Drive not found");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("drive", CollegeDtos.toDriveDto(drive));
        result.put("applicationCount", DriveApplicationRepository.INSTANCE.byDrive(drive.getId()).size());
        result.put("rounds", DriveRoundRepository.INSTANCE.byDrive(drive.getId()).stream()
                .map(CollegeDtos::toRoundDto).collect(Collectors.toList()));
        return result;
    }
}

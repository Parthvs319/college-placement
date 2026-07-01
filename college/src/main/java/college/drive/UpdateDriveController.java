package college.drive;

import helpers.annotations.CollegeRole;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.Request;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.college.CollegeAccessMiddleware;
import models.body.CollegeLoginRequest;
import models.enums.DriveStatus;
import models.json.CollegeDtos;
import models.repos.DriveRepository;
import models.sql.Drive;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;

@CollegeRole
public enum UpdateDriveController implements BaseController {

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

        Request body = request.getRequest();

        if (body.isPresent("title")) drive.title = body.get("title");
        if (body.isPresent("jobDescription")) drive.jobDescription = body.get("jobDescription");
        if (body.isPresent("jdFileUrl")) drive.jdFileUrl = body.get("jdFileUrl");
        if (body.isPresent("minCgpa")) drive.minCgpa = new BigDecimal(String.valueOf(body.get("minCgpa")));
        if (body.isPresent("maxActiveBacklogs")) drive.maxActiveBacklogs = Integer.parseInt(body.get("maxActiveBacklogs"));
        if (body.isPresent("ctcOffered")) drive.ctcOffered = new BigDecimal(String.valueOf(body.get("ctcOffered")));
        if (body.isPresent("stipend")) drive.stipend = new BigDecimal(String.valueOf(body.get("stipend")));
        if (body.isPresent("location")) drive.location = body.get("location");
        if (body.isPresent("venue")) drive.venue = body.get("venue");
        if (body.isPresent("status")) drive.status = DriveStatus.valueOf(body.get("status"));
        if (body.isPresent("registrationDeadline")) {
            drive.registrationDeadline = Timestamp.valueOf(String.valueOf(body.get("registrationDeadline")));
        }
        if (body.isPresent("driveDate")) {
            drive.driveDate = Timestamp.valueOf(String.valueOf(body.get("driveDate")));
        }

        drive.update();
        return CollegeDtos.toDriveDto(drive);
    }
}

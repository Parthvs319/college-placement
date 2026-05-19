package drive;

import helpers.annotations.UserAnnotation;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.Request;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.user.UserAccessMiddleware;
import models.body.UserLoginRequest;
import models.enums.RoundType;
import models.enums.UserType;
import models.repos.DriveRepository;
import models.sql.Drive;
import models.sql.DriveRound;

import java.sql.Timestamp;
import java.util.ArrayList;

@UserAnnotation
public enum CreateRoundController implements BaseController {

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
        if (!userType.equals(UserType.COLLEGE_ADMIN) && !userType.equals(UserType.TPO) && !userType.equals(UserType.COMPANY_HR)) {
            throw new RoutingError("Not authorized to manage rounds");
        }

        String driveIdParam = request.getRoutingContext().pathParam("driveId");
        Drive drive = DriveRepository.INSTANCE.byId(Long.parseLong(driveIdParam));
        if (drive == null) {
            throw new RoutingError("Drive not found");
        }

        Request body = request.getRequest();

        String roundTypeStr = body.get("roundType");
        String roundNumberStr = body.get("roundNumber");
        if (roundTypeStr == null || roundNumberStr == null) {
            throw new RoutingError("roundType and roundNumber are required");
        }

        DriveRound round = new DriveRound();
        round.drive = drive;
        round.roundNumber = Integer.parseInt(roundNumberStr);
        round.roundType = RoundType.valueOf(roundTypeStr);

        if (body.isPresent("name")) round.name = body.get("name");
        if (body.isPresent("description")) round.description = body.get("description");
        if (body.isPresent("venue")) round.venue = body.get("venue");
        if (body.isPresent("durationMinutes")) round.durationMinutes = Integer.parseInt(body.get("durationMinutes"));
        if (body.isPresent("scheduledAt")) round.scheduledAt = Timestamp.valueOf(String.valueOf(body.get("scheduledAt")));

        round.save();
        return round;
    }
}

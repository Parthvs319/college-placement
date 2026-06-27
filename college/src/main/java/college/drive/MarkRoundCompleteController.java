package college.drive;

import helpers.annotations.CollegeRole;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.college.CollegeAccessMiddleware;
import models.body.CollegeLoginRequest;
import models.json.CollegeDtos;
import models.repos.DriveRoundRepository;
import models.sql.DriveRound;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@CollegeRole
public enum MarkRoundCompleteController implements BaseController {

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
        String roundIdParam = request.getRoutingContext().pathParam("roundId");
        DriveRound round = DriveRoundRepository.INSTANCE.byId(Long.parseLong(roundIdParam));
        if (round == null) {
            throw new RoutingError("Round not found");
        }
        if (round.isCompleted()) {
            throw new RoutingError("Round is already marked as complete");
        }
        round.completed = true;
        round.update();

        Map<String, Object> result = new HashMap<>();
        result.put("round", CollegeDtos.toRoundDto(round));
        result.put("message", "Round marked as complete");
        return result;
    }
}

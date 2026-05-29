package college;

import helpers.annotations.CollegeRole;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.college.CollegeAccessMiddleware;
import models.body.CollegeLoginRequest;
import models.json.CollegeDtos;
import models.repos.DriveRepository;
import models.sql.Drive;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@CollegeRole
public enum ListDrivesController implements BaseController {

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
        Long collegeId = request.getCollege().getId();

        List<String> yearParam = request.getRoutingContext().queryParam("year");
        List<Drive> drives;
        if (!yearParam.isEmpty()) {
            int year = Integer.parseInt(yearParam.get(0));
            drives = DriveRepository.INSTANCE.byCollegeAndYear(collegeId, year);
        } else {
            drives = DriveRepository.INSTANCE.byCollege(collegeId);
        }

        return drives.stream().map(CollegeDtos::toDriveDto).collect(Collectors.toList());
    }
}

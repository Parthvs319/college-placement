package college;

import helpers.annotations.CollegeRole;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import lombok.Data;
import models.access.middlewear.college.CollegeAccessMiddleware;
import models.body.CollegeLoginRequest;
import models.repos.*;

import java.util.ArrayList;

@CollegeRole
public enum CollegeAnalyticsController implements BaseController {

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

        Analytics analytics = new Analytics();
        analytics.totalStudents = StudentRepository.INSTANCE.byCollege(collegeId).size();
        analytics.placedStudents = StudentRepository.INSTANCE.findPlaced(collegeId).size();
        analytics.unplacedStudents = StudentRepository.INSTANCE.findUnplaced(collegeId).size();
        analytics.totalCompanies = CompanyCollegeRepository.INSTANCE.byCollege(collegeId).size();

        if (analytics.totalStudents > 0) {
            analytics.placementRate = (double) analytics.placedStudents / analytics.totalStudents * 100;
        }

        return analytics;
    }

    @Data
    static class Analytics {
        int totalStudents;
        int placedStudents;
        int unplacedStudents;
        int totalCompanies;
        double placementRate;
    }
}

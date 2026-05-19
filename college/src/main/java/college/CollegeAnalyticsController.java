package college;

import helpers.annotations.UserAnnotation;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import lombok.Data;
import models.access.middlewear.user.UserAccessMiddleware;
import models.body.UserLoginRequest;
import models.enums.UserType;
import models.repos.*;

import java.util.ArrayList;

@UserAnnotation
public enum CollegeAnalyticsController implements BaseController {

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
        if (!userType.equals(UserType.COLLEGE_ADMIN) && !userType.equals(UserType.TPO) && !userType.equals(UserType.SUPER_ADMIN)) {
            throw new RoutingError("Not authorized to view analytics");
        }

        String collegeIdParam = request.getRoutingContext().pathParam("collegeId");
        Long collegeId = Long.parseLong(collegeIdParam);

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

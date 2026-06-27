package college.notification;

import helpers.annotations.CollegeRole;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.college.CollegeAccessMiddleware;
import models.body.CollegeLoginRequest;
import models.json.CollegeDtos;
import models.repos.NotificationRepository;
import models.sql.Notification;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * TPO / College Admin views notification history (audit log).
 *
 * GET /notifications
 * Optional query params: ?channel=EMAIL&type=DRIVE_ANNOUNCEMENT
 */
@CollegeRole
public enum ListNotificationsController implements BaseController {

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

        String channel = request.getRoutingContext().queryParams().get("channel");
        String type = request.getRoutingContext().queryParams().get("type");

        List<Notification> notifications;
        if (channel != null && !channel.isEmpty()) {
            notifications = NotificationRepository.INSTANCE.byCollegeAndChannel(collegeId, channel.toUpperCase());
        } else if (type != null && !type.isEmpty()) {
            notifications = NotificationRepository.INSTANCE.byCollegeAndType(collegeId, type.toUpperCase());
        } else {
            notifications = NotificationRepository.INSTANCE.byCollege(collegeId);
        }

        return notifications.stream().map(CollegeDtos::toNotificationDto).collect(Collectors.toList());
    }
}

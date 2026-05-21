package college;

import helpers.annotations.UserAnnotation;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.user.UserAccessMiddleware;
import models.body.UserLoginRequest;
import models.enums.UserType;
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
@UserAnnotation
public enum ListNotificationsController implements BaseController {

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
            throw new RoutingError("Not authorized to view notifications");
        }

        Long collegeId = request.getUser().college.getId();

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

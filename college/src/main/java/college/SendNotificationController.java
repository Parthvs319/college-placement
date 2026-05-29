package college;

import helpers.annotations.CollegeRole;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.college.CollegeAccessMiddleware;
import models.body.CollegeLoginRequest;
import models.json.CollegeDtos;
import models.repos.DriveRepository;
import models.repos.StudentRepository;
import models.services.NotificationService;
import models.sql.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * TPO / College Admin sends notifications to students.
 *
 * POST /notifications/send
 * Body: {
 *   "type": "DRIVE_ANNOUNCEMENT" | "DEADLINE_REMINDER" | "CUSTOM",
 *   "channels": ["EMAIL", "WHATSAPP"],
 *   "driveId": 123,              // required for DRIVE_ANNOUNCEMENT / DEADLINE_REMINDER
 *   "subject": "...",            // required for CUSTOM
 *   "body": "...",               // required for CUSTOM
 *   "studentIds": [1,2,3]        // optional — if empty, sends to all students in college
 * }
 */
@CollegeRole
public enum SendNotificationController implements BaseController {

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
        College college = request.getCollege();

        String type = request.getRequest().get("type");
        if (type == null || type.isEmpty()) {
            throw new RoutingError("Notification type is required (DRIVE_ANNOUNCEMENT, DEADLINE_REMINDER, CUSTOM)");
        }

        // Parse channels — default to EMAIL if not specified
        List<String> channels = new ArrayList<>();
        if (request.getRequest().isPresent("channels")) {
            Object channelsObj = request.getRequest().get("channels");
            if (channelsObj instanceof List) {
                channels = ((List<?>) channelsObj).stream().map(Object::toString).collect(Collectors.toList());
            }
        }
        if (channels.isEmpty()) {
            channels.add("EMAIL");
        }

        // Resolve recipients
        List<Student> recipients;
        if (request.getRequest().isPresent("studentIds")) {
            Object idsObj = request.getRequest().get("studentIds");
            if (idsObj instanceof List) {
                List<Long> studentIds = ((List<?>) idsObj).stream()
                        .map(o -> Long.parseLong(o.toString())).collect(Collectors.toList());
                recipients = studentIds.stream()
                        .map(StudentRepository.INSTANCE::byId)
                        .filter(s -> s != null && s.college.getId().equals(college.getId()))
                        .collect(Collectors.toList());
            } else {
                recipients = StudentRepository.INSTANCE.byCollege(college.getId());
            }
        } else {
            recipients = StudentRepository.INSTANCE.byCollege(college.getId());
        }

        Notification notification;

        switch (type.toUpperCase()) {
            case "DRIVE_ANNOUNCEMENT": {
                String driveIdStr = request.getRequest().get("driveId");
                if (driveIdStr == null) throw new RoutingError("driveId is required for DRIVE_ANNOUNCEMENT");
                Drive drive = DriveRepository.INSTANCE.byId(Long.parseLong(driveIdStr));
                if (drive == null) throw new RoutingError("Drive not found");
                notification = NotificationService.notifyDriveAnnouncement(drive, college, channels);
                break;
            }
            case "DEADLINE_REMINDER": {
                String driveIdStr = request.getRequest().get("driveId");
                if (driveIdStr == null) throw new RoutingError("driveId is required for DEADLINE_REMINDER");
                Drive drive = DriveRepository.INSTANCE.byId(Long.parseLong(driveIdStr));
                if (drive == null) throw new RoutingError("Drive not found");
                notification = NotificationService.notifyDeadlineReminder(drive, college, recipients, channels);
                break;
            }
            case "CUSTOM": {
                String subject = request.getRequest().get("subject");
                String body = request.getRequest().get("body");
                if (subject == null || subject.isEmpty()) throw new RoutingError("subject is required for CUSTOM");
                if (body == null || body.isEmpty()) throw new RoutingError("body is required for CUSTOM");
                Drive drive = null;
                if (request.getRequest().isPresent("driveId")) {
                    drive = DriveRepository.INSTANCE.byId(Long.parseLong(request.getRequest().get("driveId")));
                }
                notification = NotificationService.sendCustomNotification(college, drive, subject, body, channels, recipients);
                break;
            }
            default:
                throw new RoutingError("Invalid type: " + type + ". Use DRIVE_ANNOUNCEMENT, DEADLINE_REMINDER, or CUSTOM");
        }

        return CollegeDtos.toNotificationDto(notification);
    }
}

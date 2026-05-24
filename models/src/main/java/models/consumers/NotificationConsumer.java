package models.consumers;

import io.vertx.core.json.JsonObject;
import models.repos.DriveRepository;
import models.repos.OfferRepository;
import models.repos.StudentRepository;
import models.services.NotificationService;
import models.services.RabbitMQService;
import models.sql.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Consumes notification jobs from placement.notifications queue.
 *
 * Job types:
 *   DRIVE_ANNOUNCEMENT  — { driveId, collegeId, channels[] }
 *   DEADLINE_REMINDER   — { driveId, collegeId, channels[], studentIds[]? }
 *   OFFER_CREATED       — { offerId, channels[] }
 *   RESULT_PUBLISHED    — { driveId, roundName, channels[], studentIds[] }
 *   CUSTOM              — { collegeId, driveId?, subject, body, channels[], studentIds[]? }
 */
public class NotificationConsumer {

    public static void register() {
        RabbitMQService.consume(RabbitMQService.Q_NOTIFICATIONS, 5, message -> {
            String jobType = message.getString("jobType");
            JsonObject data = message.getJsonObject("data");

            System.out.println("[NotificationConsumer] Processing: " + jobType);

            switch (jobType) {
                case "DRIVE_ANNOUNCEMENT":
                    handleDriveAnnouncement(data);
                    break;
                case "DEADLINE_REMINDER":
                    handleDeadlineReminder(data);
                    break;
                case "OFFER_CREATED":
                    handleOfferCreated(data);
                    break;
                case "CUSTOM":
                    handleCustom(data);
                    break;
                default:
                    System.err.println("[NotificationConsumer] Unknown job type: " + jobType);
            }
        });
    }

    private static void handleDriveAnnouncement(JsonObject data) {
        Long driveId = data.getLong("driveId");
        Long collegeId = data.getLong("collegeId");
        List<String> channels = data.getJsonArray("channels").stream()
                .map(Object::toString).collect(Collectors.toList());

        Drive drive = DriveRepository.INSTANCE.byId(driveId);
        if (drive == null) return;

        // Use college from the drive's companyCollege link
        College college = drive.companyCollege != null ? drive.companyCollege.college : null;
        if (college == null) return;

        NotificationService.notifyDriveAnnouncement(drive, college, channels);
    }

    private static void handleDeadlineReminder(JsonObject data) {
        Long driveId = data.getLong("driveId");
        Long collegeId = data.getLong("collegeId");
        List<String> channels = data.getJsonArray("channels").stream()
                .map(Object::toString).collect(Collectors.toList());

        Drive drive = DriveRepository.INSTANCE.byId(driveId);
        if (drive == null) return;

        College college = drive.companyCollege != null ? drive.companyCollege.college : null;
        if (college == null) return;

        List<Student> recipients;
        if (data.containsKey("studentIds")) {
            recipients = data.getJsonArray("studentIds").stream()
                    .map(o -> Long.parseLong(o.toString()))
                    .map(StudentRepository.INSTANCE::byId)
                    .filter(s -> s != null)
                    .collect(Collectors.toList());
        } else {
            recipients = StudentRepository.INSTANCE.byCollege(collegeId);
        }

        NotificationService.notifyDeadlineReminder(drive, college, recipients, channels);
    }

    private static void handleOfferCreated(JsonObject data) {
        Long offerId = data.getLong("offerId");
        List<String> channels = data.getJsonArray("channels").stream()
                .map(Object::toString).collect(Collectors.toList());

        Offer offer = OfferRepository.INSTANCE.byId(offerId);
        if (offer == null) return;

        NotificationService.notifyOfferCreated(offer, channels);
    }

    private static void handleCustom(JsonObject data) {
        Long collegeId = data.getLong("collegeId");
        String subject = data.getString("subject");
        String body = data.getString("body");
        List<String> channels = data.getJsonArray("channels").stream()
                .map(Object::toString).collect(Collectors.toList());

        Drive drive = null;
        if (data.containsKey("driveId")) {
            drive = DriveRepository.INSTANCE.byId(data.getLong("driveId"));
        }

        // Resolve college
        College college = models.repos.CollegeRepository.INSTANCE.byId(collegeId);
        if (college == null) return;

        List<Student> recipients;
        if (data.containsKey("studentIds")) {
            recipients = data.getJsonArray("studentIds").stream()
                    .map(o -> Long.parseLong(o.toString()))
                    .map(StudentRepository.INSTANCE::byId)
                    .filter(s -> s != null)
                    .collect(Collectors.toList());
        } else {
            recipients = StudentRepository.INSTANCE.byCollege(collegeId);
        }

        NotificationService.sendCustomNotification(college, drive, subject, body, channels, recipients);
    }
}

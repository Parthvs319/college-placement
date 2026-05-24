package models.services;

import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.Vertx;
import models.enums.DriveStatus;
import models.enums.OfferStatus;
import models.sql.Drive;
import models.sql.Offer;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

/**
 * Scheduler service using Vert.x periodic timers.
 * Runs automated lifecycle management jobs:
 *
 *   - Offer expiry check (every hour)
 *   - Drive registration close (every hour)
 *   - Deadline reminder notifications (every 6 hours)
 *
 * Jobs are published to RabbitMQ queues for reliable processing.
 * If RabbitMQ is not configured, jobs run synchronously.
 */
public class SchedulerService {

    private static final long ONE_HOUR_MS = 60 * 60 * 1000;
    private static final long SIX_HOURS_MS = 6 * ONE_HOUR_MS;

    /**
     * Start all scheduled jobs. Call once after Vertx is deployed.
     */
    public static void start(Vertx vertx) {
        System.out.println("[Scheduler] Starting scheduled jobs...");

        // ── Offer Expiry — every hour ──
        vertx.setPeriodic(ONE_HOUR_MS, id -> {
            System.out.println("[Scheduler] Running: offer expiry check");
            checkOfferExpiry();
        });

        // ── Drive Registration Close — every hour ──
        vertx.setPeriodic(ONE_HOUR_MS, id -> {
            System.out.println("[Scheduler] Running: drive registration close check");
            checkDriveRegistrationClose();
        });

        // ── Deadline Reminders — every 6 hours ──
        vertx.setPeriodic(SIX_HOURS_MS, id -> {
            System.out.println("[Scheduler] Running: deadline reminder check");
            sendDeadlineReminders();
        });

        System.out.println("[Scheduler] All jobs scheduled");
    }

    // ── Offer Expiry ─────────────────────────────────────────────────

    /**
     * Find all PENDING offers past their responseDeadline and expire them.
     */
    private static void checkOfferExpiry() {
        Timestamp now = new Timestamp(System.currentTimeMillis());

        List<Offer> expiredOffers = models.repos.OfferRepository.INSTANCE.where()
                .eq("status", OfferStatus.PENDING)
                .lt("responseDeadline", now)
                .findList();

        for (Offer offer : expiredOffers) {
            if (RabbitMQService.isInitialized()) {
                RabbitMQService.publish(RabbitMQService.Q_APPLICATIONS, "OFFER_EXPIRED", offer.getId());
            } else {
                // Synchronous fallback
                offer.status = OfferStatus.EXPIRED;
                offer.update();
                System.out.println("[Scheduler] Offer " + offer.getId() + " expired (sync)");
            }
        }

        if (!expiredOffers.isEmpty()) {
            System.out.println("[Scheduler] Expired " + expiredOffers.size() + " offers");
        }
    }

    // ── Drive Registration Close ─────────────────────────────────────

    /**
     * Find all REGISTRATION_OPEN drives past their registrationDeadline and close them.
     */
    private static void checkDriveRegistrationClose() {
        Timestamp now = new Timestamp(System.currentTimeMillis());

        List<Drive> expiredDrives = models.repos.DriveRepository.INSTANCE.where()
                .eq("status", DriveStatus.REGISTRATION_OPEN)
                .lt("registrationDeadline", now)
                .findList();

        for (Drive drive : expiredDrives) {
            if (RabbitMQService.isInitialized()) {
                RabbitMQService.publish(RabbitMQService.Q_APPLICATIONS, "CLOSE_REGISTRATION", drive.getId());
            } else {
                drive.status = DriveStatus.REGISTRATION_CLOSED;
                drive.update();
                System.out.println("[Scheduler] Drive " + drive.getId() + " registration closed (sync)");
            }
        }

        if (!expiredDrives.isEmpty()) {
            System.out.println("[Scheduler] Closed registration for " + expiredDrives.size() + " drives");
        }
    }

    // ── Deadline Reminders ───────────────────────────────────────────

    /**
     * Find drives with registration closing in the next 24 hours
     * and send reminders to eligible students who haven't applied.
     */
    private static void sendDeadlineReminders() {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        Timestamp in24h = new Timestamp(now.getTime() + (24 * ONE_HOUR_MS));

        List<Drive> closingSoon = models.repos.DriveRepository.INSTANCE.where()
                .in("status", DriveStatus.UPCOMING, DriveStatus.REGISTRATION_OPEN)
                .gt("registrationDeadline", now)
                .lt("registrationDeadline", in24h)
                .findList();

        for (Drive drive : closingSoon) {
            if (drive.companyCollege == null || drive.companyCollege.college == null) continue;

            Long collegeId = drive.companyCollege.college.getId();

            if (RabbitMQService.isInitialized()) {
                RabbitMQService.publish(RabbitMQService.Q_NOTIFICATIONS, "DEADLINE_REMINDER",
                        new JsonObject()
                                .put("driveId", drive.getId())
                                .put("collegeId", collegeId)
                                .put("channels", Arrays.asList("EMAIL"))
                );
            } else {
                // Synchronous fallback
                List<models.sql.Student> students = models.repos.StudentRepository.INSTANCE.byCollege(collegeId);
                NotificationService.notifyDeadlineReminder(drive, drive.companyCollege.college,
                        students, Arrays.asList("EMAIL"));
            }
        }

        if (!closingSoon.isEmpty()) {
            System.out.println("[Scheduler] Sent deadline reminders for " + closingSoon.size() + " drives");
        }
    }
}

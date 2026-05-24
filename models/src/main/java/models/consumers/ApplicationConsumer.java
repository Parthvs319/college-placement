package models.consumers;

import io.vertx.core.json.JsonObject;
import models.enums.ApplicationStatus;
import models.enums.DriveStatus;
import models.enums.OfferStatus;
import models.repos.*;
import models.services.RabbitMQService;
import models.sql.*;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;

/**
 * Consumes application lifecycle jobs from placement.applications queue.
 *
 * Job types:
 *   CLOSE_REGISTRATION   — { driveId } → close registration, notify un-applied eligible students
 *   OFFER_ACCEPTED       — { offerId } → enforce placement policy (block further drives, mark placed)
 *   OFFER_EXPIRED        — { offerId } → auto-expire offer past deadline
 *   AUTO_FILTER_DRIVE    — { driveId } → auto-exclude ineligible students
 */
public class ApplicationConsumer {

    public static void register() {
        RabbitMQService.consume(RabbitMQService.Q_APPLICATIONS, 3, message -> {
            String jobType = message.getString("jobType");
            JsonObject data = message.getJsonObject("data");

            System.out.println("[ApplicationConsumer] Processing: " + jobType);

            switch (jobType) {
                case "CLOSE_REGISTRATION":
                    handleCloseRegistration(data);
                    break;
                case "OFFER_ACCEPTED":
                    handleOfferAccepted(data);
                    break;
                case "OFFER_EXPIRED":
                    handleOfferExpired(data);
                    break;
                default:
                    System.err.println("[ApplicationConsumer] Unknown job type: " + jobType);
            }
        });
    }

    private static void handleCloseRegistration(JsonObject data) {
        Long driveId = data.getLong("driveId");
        Drive drive = DriveRepository.INSTANCE.byId(driveId);
        if (drive == null) return;

        if (drive.status == DriveStatus.REGISTRATION_OPEN) {
            drive.status = DriveStatus.REGISTRATION_CLOSED;
            drive.update();
            System.out.println("[ApplicationConsumer] Registration closed for drive " + driveId);

            // Notify eligible students who didn't apply
            if (drive.companyCollege != null && drive.companyCollege.college != null) {
                College college = drive.companyCollege.college;
                List<Student> eligible = StudentRepository.INSTANCE.findEligible(
                        college.getId(),
                        drive.minCgpa,
                        drive.maxActiveBacklogs,
                        drive.eligibleDepartments,
                        drive.academicYear
                );

                // Filter out those who already applied
                eligible.removeIf(s -> {
                    DriveApplication app = DriveApplicationRepository.INSTANCE
                            .byStudentAndDrive(s.getId(), driveId);
                    return app != null;
                });

                if (!eligible.isEmpty()) {
                    // Queue notification about missed opportunity
                    RabbitMQService.publish(RabbitMQService.Q_NOTIFICATIONS, "CUSTOM",
                            new JsonObject()
                                    .put("collegeId", college.getId())
                                    .put("driveId", driveId)
                                    .put("subject", "Registration Closed: " + drive.title)
                                    .put("body", "Registration for this drive has closed. Keep an eye out for upcoming drives!")
                                    .put("channels", Arrays.asList("EMAIL"))
                                    .put("studentIds", eligible.stream().map(Student::getId).toList())
                    );
                }
            }
        }
    }

    private static void handleOfferAccepted(JsonObject data) {
        Long offerId = data.getLong("offerId");
        Offer offer = OfferRepository.INSTANCE.byId(offerId);
        if (offer == null || offer.status != OfferStatus.ACCEPTED) return;

        Student student = offer.student;
        if (student == null) return;

        // Mark student as placed
        student.placed = true;
        student.currentCtc = offer.ctcOffered;
        student.update();

        // Check placement policy
        College college = student.college;
        if (college == null) return;

        PlacementPolicy policy = PlacementPolicyRepository.INSTANCE
                .byCollegeAndYear(college.getId(), offer.drive != null ? offer.drive.academicYear : 0);

        if (policy != null && policy.blockAfterFirstAccept) {
            // Withdraw all other pending applications
            List<DriveApplication> otherApps = DriveApplicationRepository.INSTANCE.byStudent(student.getId());
            for (DriveApplication app : otherApps) {
                if (app.drive != null && !app.drive.getId().equals(offer.drive.getId())
                        && app.status != ApplicationStatus.REJECTED
                        && app.status != ApplicationStatus.WITHDRAWN) {
                    app.status = ApplicationStatus.WITHDRAWN;
                    app.notes = (app.notes != null ? app.notes + "\n" : "")
                            + "Auto-withdrawn: student accepted offer from "
                            + (offer.drive.companyCollege != null && offer.drive.companyCollege.company != null
                            ? offer.drive.companyCollege.company.name : "another company");
                    app.update();
                }
            }

            // Decline other pending offers
            List<Offer> otherOffers = OfferRepository.INSTANCE.pendingByStudent(student.getId());
            for (Offer o : otherOffers) {
                if (!o.getId().equals(offerId)) {
                    o.status = OfferStatus.DECLINED;
                    o.respondedAt = new Timestamp(System.currentTimeMillis());
                    o.notes = (o.notes != null ? o.notes + "\n" : "")
                            + "Auto-declined: student accepted another offer";
                    o.update();
                }
            }

            System.out.println("[ApplicationConsumer] Policy enforced for student " + student.getId()
                    + " — blocked after first accept");
        }

        // Check dream CTC threshold
        if (policy != null && policy.dreamCtcThreshold != null
                && offer.ctcOffered != null
                && offer.ctcOffered.compareTo(policy.dreamCtcThreshold) >= 0) {
            System.out.println("[ApplicationConsumer] Student " + student.getId()
                    + " got dream offer (" + offer.ctcOffered + " >= " + policy.dreamCtcThreshold + ")");
            // Dream offer — same behavior as blockAfterFirstAccept
        }
    }

    private static void handleOfferExpired(JsonObject data) {
        Long offerId = data.getLong("offerId");
        Offer offer = OfferRepository.INSTANCE.byId(offerId);
        if (offer == null || offer.status != OfferStatus.PENDING) return;

        if (offer.responseDeadline != null
                && offer.responseDeadline.before(new Timestamp(System.currentTimeMillis()))) {
            offer.status = OfferStatus.EXPIRED;
            offer.update();
            System.out.println("[ApplicationConsumer] Offer " + offerId + " expired");
        }
    }
}

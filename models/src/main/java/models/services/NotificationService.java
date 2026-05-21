package models.services;

import models.repos.StudentRepository;
import models.sql.*;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Orchestrates sending notifications via Email and/or WhatsApp.
 * Resolves recipients, dispatches messages, and logs to the Notification entity.
 *
 * Usage (from controllers):
 *   NotificationService.notifyDriveAnnouncement(drive, college, channels);
 *   NotificationService.notifyOfferCreated(offer, channels);
 *   NotificationService.sendCustomNotification(college, drive, subject, body, channels, students);
 */
public class NotificationService {

    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("dd MMM yyyy, hh:mm a");
    private static final String PORTAL_URL = System.getenv().getOrDefault("PORTAL_URL", "https://placement.example.com");

    // ── Drive Announcement ───────────────────────────────────────────

    /**
     * Notify all eligible students about a new/updated drive.
     */
    public static Notification notifyDriveAnnouncement(Drive drive, College college, List<String> channels) {
        String companyName = drive.companyCollege != null && drive.companyCollege.company != null
                ? drive.companyCollege.company.name : "Unknown";
        String driveDate = drive.driveDate != null ? DATE_FMT.format(drive.driveDate) : "TBD";
        String regDeadline = drive.registrationDeadline != null ? DATE_FMT.format(drive.registrationDeadline) : "TBD";

        List<Student> students = StudentRepository.INSTANCE.byCollege(college.getId());

        AtomicInteger delivered = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);

        for (Student student : students) {
            if (student.user == null || !student.user.verified) continue;

            for (String channel : channels) {
                try {
                    if ("EMAIL".equalsIgnoreCase(channel) && student.user.email != null) {
                        String html = EmailService.buildDriveAnnouncementHtml(
                                companyName, drive.title, driveDate, regDeadline, college.name, PORTAL_URL);
                        EmailService.sendEmail(student.user.email,
                                "New Drive: " + companyName + " — " + drive.title, html)
                                .subscribe(ok -> { if (ok) delivered.incrementAndGet(); else failed.incrementAndGet(); },
                                        err -> failed.incrementAndGet());
                    }
                    if ("WHATSAPP".equalsIgnoreCase(channel) && student.user.mobile != null) {
                        String msg = WhatsAppService.buildDriveAnnouncement(
                                companyName, drive.title, driveDate, regDeadline, college.name);
                        WhatsAppService.sendMessage(student.user.mobile, msg)
                                .subscribe(ok -> { if (ok) delivered.incrementAndGet(); else failed.incrementAndGet(); },
                                        err -> failed.incrementAndGet());
                    }
                } catch (Exception e) {
                    failed.incrementAndGet();
                }
            }
        }

        return logNotification(college, drive, channels, "DRIVE_ANNOUNCEMENT",
                "New Drive: " + companyName + " — " + drive.title, null,
                students.size(), delivered.get(), failed.get(), null);
    }

    // ── Offer Notification ───────────────────────────────────────────

    /**
     * Notify a student about a new offer.
     */
    public static Notification notifyOfferCreated(Offer offer, List<String> channels) {
        Student student = offer.student;
        Drive drive = offer.drive;
        College college = student.college;
        String companyName = drive.companyCollege != null && drive.companyCollege.company != null
                ? drive.companyCollege.company.name : "Unknown";
        String studentName = student.user != null ? student.user.name : "Student";
        String ctc = offer.ctcOffered != null ? offer.ctcOffered.toPlainString() : "N/A";
        String designation = offer.designation != null ? offer.designation : "N/A";
        String deadline = offer.responseDeadline != null ? DATE_FMT.format(offer.responseDeadline) : "TBD";

        AtomicInteger delivered = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);

        for (String channel : channels) {
            try {
                if ("EMAIL".equalsIgnoreCase(channel) && student.user != null && student.user.email != null) {
                    String html = EmailService.buildOfferNotificationHtml(
                            studentName, companyName, designation, ctc, deadline, PORTAL_URL);
                    EmailService.sendEmail(student.user.email,
                            "Offer from " + companyName + " — " + designation, html)
                            .subscribe(ok -> { if (ok) delivered.incrementAndGet(); else failed.incrementAndGet(); },
                                    err -> failed.incrementAndGet());
                }
                if ("WHATSAPP".equalsIgnoreCase(channel) && student.user != null && student.user.mobile != null) {
                    String msg = WhatsAppService.buildOfferNotification(
                            studentName, companyName, designation, ctc, deadline);
                    WhatsAppService.sendMessage(student.user.mobile, msg)
                            .subscribe(ok -> { if (ok) delivered.incrementAndGet(); else failed.incrementAndGet(); },
                                    err -> failed.incrementAndGet());
                }
            } catch (Exception e) {
                failed.incrementAndGet();
            }
        }

        return logNotification(college, drive, channels, "OFFER",
                "Offer: " + companyName + " — " + designation, null,
                1, delivered.get(), failed.get(), null);
    }

    // ── Deadline Reminder ────────────────────────────────────────────

    /**
     * Remind students who haven't applied yet about an upcoming deadline.
     */
    public static Notification notifyDeadlineReminder(Drive drive, College college,
                                                       List<Student> recipients, List<String> channels) {
        String companyName = drive.companyCollege != null && drive.companyCollege.company != null
                ? drive.companyCollege.company.name : "Unknown";
        String deadline = drive.registrationDeadline != null ? DATE_FMT.format(drive.registrationDeadline) : "TBD";

        AtomicInteger delivered = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);

        for (Student student : recipients) {
            if (student.user == null) continue;
            String studentName = student.user.name != null ? student.user.name : "Student";

            for (String channel : channels) {
                try {
                    if ("EMAIL".equalsIgnoreCase(channel) && student.user.email != null) {
                        String html = EmailService.buildDeadlineReminderHtml(
                                studentName, companyName, drive.title, deadline, PORTAL_URL);
                        EmailService.sendEmail(student.user.email,
                                "Deadline Reminder: " + companyName + " — " + drive.title, html)
                                .subscribe(ok -> { if (ok) delivered.incrementAndGet(); else failed.incrementAndGet(); },
                                        err -> failed.incrementAndGet());
                    }
                    if ("WHATSAPP".equalsIgnoreCase(channel) && student.user.mobile != null) {
                        String msg = WhatsAppService.buildDeadlineReminder(
                                studentName, companyName, drive.title, deadline);
                        WhatsAppService.sendMessage(student.user.mobile, msg)
                                .subscribe(ok -> { if (ok) delivered.incrementAndGet(); else failed.incrementAndGet(); },
                                        err -> failed.incrementAndGet());
                    }
                } catch (Exception e) {
                    failed.incrementAndGet();
                }
            }
        }

        return logNotification(college, drive, channels, "DEADLINE_REMINDER",
                "Deadline Reminder: " + companyName + " — " + drive.title, null,
                recipients.size(), delivered.get(), failed.get(), null);
    }

    // ── Custom Notification ──────────────────────────────────────────

    /**
     * TPO sends a custom notification to selected students (or all).
     */
    public static Notification sendCustomNotification(College college, Drive drive,
                                                       String subject, String body,
                                                       List<String> channels, List<Student> recipients) {
        AtomicInteger delivered = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);

        for (Student student : recipients) {
            if (student.user == null) continue;
            String studentName = student.user.name != null ? student.user.name : "Student";

            for (String channel : channels) {
                try {
                    if ("EMAIL".equalsIgnoreCase(channel) && student.user.email != null) {
                        String html = EmailService.buildCustomHtml(studentName, subject, body);
                        EmailService.sendEmail(student.user.email, subject, html)
                                .subscribe(ok -> { if (ok) delivered.incrementAndGet(); else failed.incrementAndGet(); },
                                        err -> failed.incrementAndGet());
                    }
                    if ("WHATSAPP".equalsIgnoreCase(channel) && student.user.mobile != null) {
                        String msg = WhatsAppService.buildCustomMessage(studentName, subject, body);
                        WhatsAppService.sendMessage(student.user.mobile, msg)
                                .subscribe(ok -> { if (ok) delivered.incrementAndGet(); else failed.incrementAndGet(); },
                                        err -> failed.incrementAndGet());
                    }
                } catch (Exception e) {
                    failed.incrementAndGet();
                }
            }
        }

        return logNotification(college, drive, channels, "CUSTOM", subject, body,
                recipients.size(), delivered.get(), failed.get(), null);
    }

    // ── Audit Log ────────────────────────────────────────────────────

    private static Notification logNotification(College college, Drive drive, List<String> channels,
                                                 String type, String subject, String body,
                                                 int recipientCount, int deliveredCount, int failedCount,
                                                 Map<String, String> metadata) {
        Notification notification = new Notification();
        notification.college = college;
        notification.drive = drive;
        notification.channel = String.join(",", channels);
        notification.type = type;
        notification.subject = subject;
        notification.body = body;
        notification.recipientCount = recipientCount;
        notification.deliveredCount = deliveredCount;
        notification.failedCount = failedCount;
        notification.sentAt = new Timestamp(System.currentTimeMillis());
        notification.metadata = metadata;
        notification.save();
        return notification;
    }
}

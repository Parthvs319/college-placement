package superadmin;

import helpers.annotations.SuperAdminRole;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.superadmin.SuperAdminAccessMiddleware;
import models.repos.*;
import models.sql.*;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * GET /admin/activity?limit=20
 * Returns a unified timeline of recent platform events derived from existing tables.
 * No separate event log table needed - we query recent records by createdAt.
 */
@SuperAdminRole
public enum PlatformActivityController implements BaseController {

    INSTANCE;

    @Override
    public void handle(RoutingContext event) {
        SuperAdminAccessMiddleware.INSTANCE.with(event, new ArrayList<>(), this.getClass())
                .map(req -> {
                    String limitParam = event.request().getParam("limit");
                    int limit = 20;
                    if (limitParam != null && !limitParam.isEmpty()) {
                        limit = Math.min(Integer.parseInt(limitParam), 50);
                    }

                    List<SuperAdminDtos.ActivityEvent> activities = new ArrayList<>();

                    // 1. Recent colleges - registered, verified, activated/deactivated
                    List<College> recentColleges = CollegeRepository.INSTANCE.findRecent(10);
                    for (College c : recentColleges) {
                        String cityName = "";
                        if (c.cityId != null) {
                            City city = CityRepository.INSTANCE.byId(c.cityId);
                            if (city != null) cityName = ", " + city.name;
                        }
                        String desc = c.name + " (" + c.code + ")" + cityName;

                        // Registration event (uses createdAt)
                        SuperAdminDtos.ActivityEvent regEvent = new SuperAdminDtos.ActivityEvent();
                        if (c.verified) {
                            regEvent.setType("college_verified");
                            regEvent.setTitle("College verified");
                            regEvent.setColor("green");
                        } else {
                            regEvent.setType("college_added");
                            regEvent.setTitle("New college registered");
                            regEvent.setColor("amber");
                        }
                        regEvent.setDescription(desc);
                        regEvent.setTimestamp(tsToString(c.getCreatedAt()));
                        activities.add(regEvent);

                        // If updatedAt differs from createdAt, college was toggled - show activate/deactivate
                        if (c.getUpdatedAt() != null && c.getCreatedAt() != null
                                && c.getUpdatedAt().getTime() - c.getCreatedAt().getTime() > 5000) {
                            SuperAdminDtos.ActivityEvent toggleEvent = new SuperAdminDtos.ActivityEvent();
                            if (c.active) {
                                toggleEvent.setType("college_activated");
                                toggleEvent.setTitle("College activated");
                                toggleEvent.setColor("green");
                            } else {
                                toggleEvent.setType("college_deactivated");
                                toggleEvent.setTitle("College deactivated");
                                toggleEvent.setColor("amber");
                            }
                            toggleEvent.setDescription(desc);
                            toggleEvent.setTimestamp(tsToString(c.getUpdatedAt()));
                            activities.add(toggleEvent);
                        }
                    }

                    List<Drive> recentDrives = DriveRepository.INSTANCE.findRecent(10);
                    for (Drive d : recentDrives) {
                        SuperAdminDtos.ActivityEvent e = new SuperAdminDtos.ActivityEvent();
                        String companyName = "";
                        String collegeName = "";
                        if (d.companyCollege != null) {
                            if (d.companyCollege.getCompany() != null) companyName = d.companyCollege.getCompany().name;
                            if (d.companyCollege.getCollege() != null) collegeName = d.companyCollege.getCollege().name;
                        }

                        if (d.status != null && d.status.name().equals("COMPLETED")) {
                            e.setType("drive_completed");
                            e.setTitle("Drive completed");
                            e.setColor("green");
                        } else {
                            e.setType("drive_created");
                            e.setTitle("New drive created");
                            e.setColor("blue");
                        }
                        e.setDescription(d.title + " by " + companyName + " at " + collegeName);
                        e.setTimestamp(tsToString(d.getCreatedAt()));
                        activities.add(e);
                    }

                    List<Offer> recentOffers = OfferRepository.INSTANCE.findRecent(10);
                    for (Offer o : recentOffers) {
                        SuperAdminDtos.ActivityEvent e = new SuperAdminDtos.ActivityEvent();
                        e.setType("offer_made");
                        e.setTitle("Offer extended");
                        e.setColor("green");

                        String studentName = "";
                        String companyName = "";
                        if (o.student != null && o.student.user != null) studentName = o.student.user.name;
                        if (o.drive != null && o.drive.companyCollege != null && o.drive.companyCollege.getCompany() != null) {
                            companyName = o.drive.companyCollege.getCompany().name;
                        }
                        String ctcStr = o.ctcOffered != null ? " - " + o.ctcOffered.toPlainString() + " LPA" : "";
                        e.setDescription(studentName + " from " + companyName + ctcStr);
                        e.setTimestamp(tsToString(o.getCreatedAt()));
                        activities.add(e);
                    }

                    List<DriveApplication> recentApps = DriveApplicationRepository.INSTANCE.findRecent(10);
                    for (DriveApplication app : recentApps) {
                        SuperAdminDtos.ActivityEvent e = new SuperAdminDtos.ActivityEvent();
                        e.setType("application_submitted");
                        e.setTitle("New application");
                        e.setColor("blue");

                        String studentName = "";
                        String driveTitle = "";
                        if (app.student != null && app.student.user != null) studentName = app.student.user.name;
                        if (app.drive != null) driveTitle = app.drive.title;
                        e.setDescription(studentName + " applied to " + driveTitle);
                        e.setTimestamp(tsToString(app.getCreatedAt()));
                        activities.add(e);
                    }

                    // 5. Recent users - registered, activated/deactivated
                    List<User> recentUsers = UserRepository.INSTANCE.findRecent(10);
                    for (User u : recentUsers) {
                        String role = u.userType != null ? u.userType.name() : "USER";
                        String desc = u.name + " (" + role.replace("_", " ") + ")";

                        // Registration event
                        SuperAdminDtos.ActivityEvent regEvent = new SuperAdminDtos.ActivityEvent();
                        regEvent.setType("user_registered");
                        regEvent.setTitle("User registered");
                        regEvent.setColor("purple");
                        regEvent.setDescription(desc);
                        regEvent.setTimestamp(tsToString(u.getCreatedAt()));
                        activities.add(regEvent);

                        // If updatedAt differs from createdAt, user was toggled
                        if (u.getUpdatedAt() != null && u.getCreatedAt() != null
                                && u.getUpdatedAt().getTime() - u.getCreatedAt().getTime() > 5000) {
                            SuperAdminDtos.ActivityEvent toggleEvent = new SuperAdminDtos.ActivityEvent();
                            if (u.active) {
                                toggleEvent.setType("user_activated");
                                toggleEvent.setTitle("User activated");
                                toggleEvent.setColor("green");
                            } else {
                                toggleEvent.setType("user_deactivated");
                                toggleEvent.setTitle("User deactivated");
                                toggleEvent.setColor("amber");
                            }
                            toggleEvent.setDescription(desc);
                            toggleEvent.setTimestamp(tsToString(u.getUpdatedAt()));
                            activities.add(toggleEvent);
                        }
                    }

                    // 6. Recent subscriptions
                    List<Subscription> recentSubs = SubscriptionRepository.INSTANCE.findRecent(10);
                    for (Subscription s : recentSubs) {
                        SuperAdminDtos.ActivityEvent e = new SuperAdminDtos.ActivityEvent();
                        String studentName = "";
                        String collegeName = "";
                        if (s.student != null && s.student.user != null) studentName = s.student.user.name;
                        if (s.college != null) collegeName = s.college.name;

                        String tierStr = s.tier != null ? s.tier.name() : "FREE";
                        if (s.active) {
                            e.setType("subscription_created");
                            e.setTitle("New " + tierStr.toLowerCase() + " subscription");
                            e.setColor("green");
                        } else {
                            e.setType("subscription_expired");
                            e.setTitle("Subscription expired");
                            e.setColor("amber");
                        }

                        if (!studentName.isEmpty()) {
                            e.setDescription(studentName + " - " + tierStr);
                        } else if (!collegeName.isEmpty()) {
                            e.setDescription(collegeName + " - " + tierStr);
                        } else {
                            e.setDescription(tierStr + " subscription");
                        }
                        e.setTimestamp(tsToString(s.getCreatedAt()));
                        activities.add(e);
                    }

                    activities.sort((a, b) -> {
                        if (a.getTimestamp() == null && b.getTimestamp() == null) return 0;
                        if (a.getTimestamp() == null) return 1;
                        if (b.getTimestamp() == null) return -1;
                        return b.getTimestamp().compareTo(a.getTimestamp());
                    });

                    return activities.stream().limit(limit).collect(Collectors.toList());
                })
                .subscribe(
                        o -> ResponseUtils.INSTANCE.writeJsonResponse(event, o),
                        error -> ResponseUtils.INSTANCE.handleError(event, error)
                );
    }

    private String tsToString(Timestamp ts) {
        if (ts == null) return null;
        return ts.toInstant().toString();
    }
}

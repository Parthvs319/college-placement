package superadmin;

import helpers.annotations.SuperAdminRole;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.ebean.DB;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.superadmin.SuperAdminAccessMiddleware;
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

                    // 1. Recent colleges added
                    List<College> recentColleges = DB.find(College.class)
                            .orderBy("createdAt desc")
                            .setMaxRows(10)
                            .findList();
                    for (College c : recentColleges) {
                        SuperAdminDtos.ActivityEvent e = new SuperAdminDtos.ActivityEvent();
                        if (c.verified) {
                            e.setType("college_verified");
                            e.setTitle("College verified");
                            e.setColor("green");
                        } else {
                            e.setType("college_added");
                            e.setTitle("New college registered");
                            e.setColor("amber");
                        }
                        String cityName = "";
                        if (c.cityId != null) {
                            City city = DB.find(City.class, c.cityId);
                            if (city != null) cityName = ", " + city.name;
                        }
                        e.setDescription(c.name + " (" + c.code + ")" + cityName);
                        e.setTimestamp(tsToString(c.getCreatedAt()));
                        activities.add(e);
                    }

                    // 2. Recent drives created
                    List<Drive> recentDrives = DB.find(Drive.class)
                            .fetch("companyCollege")
                            .fetch("companyCollege.company")
                            .fetch("companyCollege.college")
                            .orderBy("createdAt desc")
                            .setMaxRows(10)
                            .findList();
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

                    // 3. Recent offers made
                    List<Offer> recentOffers = DB.find(Offer.class)
                            .fetch("student")
                            .fetch("student.user")
                            .fetch("drive")
                            .fetch("drive.companyCollege")
                            .fetch("drive.companyCollege.company")
                            .orderBy("createdAt desc")
                            .setMaxRows(10)
                            .findList();
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

                    // 4. Recent applications
                    List<DriveApplication> recentApps = DB.find(DriveApplication.class)
                            .fetch("student")
                            .fetch("student.user")
                            .fetch("drive")
                            .orderBy("createdAt desc")
                            .setMaxRows(10)
                            .findList();
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

                    // 5. Recent user registrations
                    List<User> recentUsers = DB.find(User.class)
                            .orderBy("createdAt desc")
                            .setMaxRows(10)
                            .findList();
                    for (User u : recentUsers) {
                        SuperAdminDtos.ActivityEvent e = new SuperAdminDtos.ActivityEvent();
                        e.setType("user_registered");
                        e.setTitle("User registered");
                        e.setColor("purple");
                        String role = u.userType != null ? u.userType.name() : "USER";
                        e.setDescription(u.name + " (" + role.replace("_", " ") + ")");
                        e.setTimestamp(tsToString(u.getCreatedAt()));
                        activities.add(e);
                    }

                    // Sort all by timestamp descending and take limit
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

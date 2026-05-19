package models.repos;

import helpers.sql.SqlFinder;
import io.ebean.ExpressionList;
import models.sql.Notification;

import java.util.List;

public enum NotificationRepository {
    INSTANCE;

    private final SqlFinder<Long, Notification> finder = new SqlFinder<>(Notification.class);

    public Notification byId(Long id) {
        return finder.query().where().eq("id", id).eq("deleted", false).findOne();
    }

    public List<Notification> byCollege(Long collegeId) {
        return finder.query().where()
                .eq("college.id", collegeId)
                .eq("deleted", false)
                .orderBy("createdAt desc")
                .findList();
    }

    public List<Notification> byDrive(Long driveId) {
        return finder.query().where()
                .eq("drive.id", driveId)
                .eq("deleted", false)
                .orderBy("createdAt desc")
                .findList();
    }

    public List<Notification> byCollegeAndChannel(Long collegeId, String channel) {
        return finder.query().where()
                .eq("college.id", collegeId)
                .eq("channel", channel)
                .eq("deleted", false)
                .orderBy("createdAt desc")
                .findList();
    }

    public List<Notification> byCollegeAndType(Long collegeId, String type) {
        return finder.query().where()
                .eq("college.id", collegeId)
                .eq("type", type)
                .eq("deleted", false)
                .orderBy("createdAt desc")
                .findList();
    }

    public ExpressionList<Notification> where() {
        return finder.query().where().eq("deleted", false);
    }
}

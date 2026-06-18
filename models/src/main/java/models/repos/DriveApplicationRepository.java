package models.repos;

import helpers.sql.SqlFinder;
import io.ebean.ExpressionList;
import models.enums.ApplicationStatus;
import models.sql.DriveApplication;

import java.util.List;

public enum DriveApplicationRepository {
    INSTANCE;

    private final SqlFinder<Long, DriveApplication> finder = new SqlFinder<>(DriveApplication.class);

    public DriveApplication byId(Long id) {
        return finder.query().where().eq("id", id).eq("deleted", false).findOne();
    }

    public DriveApplication byStudentAndDrive(Long studentId, Long driveId) {
        return finder.query().where()
                .eq("student.id", studentId)
                .eq("drive.id", driveId)
                .eq("deleted", false)
                .findOne();
    }

    public List<DriveApplication> byDrive(Long driveId) {
        return finder.query().where()
                .eq("drive.id", driveId)
                .eq("deleted", false)
                .findList();
    }

    public List<DriveApplication> byDriveAndStatus(Long driveId, ApplicationStatus status) {
        return finder.query().where()
                .eq("drive.id", driveId)
                .eq("status", status)
                .eq("deleted", false)
                .findList();
    }

    public List<DriveApplication> byStudent(Long studentId) {
        return finder.query().where()
                .eq("student.id", studentId)
                .eq("deleted", false)
                .orderBy("createdAt desc")
                .findList();
    }

    public int countByDrive(Long driveId) {
        return finder.query().where()
                .eq("drive.id", driveId)
                .eq("deleted", false)
                .findCount();
    }

    public int countByDriveAndStatus(Long driveId, ApplicationStatus status) {
        return finder.query().where()
                .eq("drive.id", driveId)
                .eq("status", status)
                .eq("deleted", false)
                .findCount();
    }

    public List<DriveApplication> findRecent(int limit) {
        return finder.query()
                .fetch("student")
                .fetch("student.user")
                .fetch("drive")
                .where().eq("deleted", false)
                .orderBy("createdAt desc")
                .setMaxRows(limit)
                .findList();
    }

    public ExpressionList<DriveApplication> where() {
        return finder.query().where().eq("deleted", false);
    }
}

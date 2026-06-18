package models.repos;

import helpers.sql.SqlFinder;
import io.ebean.ExpressionList;
import models.enums.OfferStatus;
import models.sql.Offer;

import java.util.List;

public enum OfferRepository {
    INSTANCE;

    private final SqlFinder<Long, Offer> finder = new SqlFinder<>(Offer.class);

    public Offer byId(Long id) {
        return finder.query().where().eq("id", id).eq("deleted", false).findOne();
    }

    public Offer byStudentAndDrive(Long studentId, Long driveId) {
        return finder.query().where()
                .eq("student.id", studentId)
                .eq("drive.id", driveId)
                .eq("deleted", false)
                .findOne();
    }

    public List<Offer> byStudent(Long studentId) {
        return finder.query().where()
                .eq("student.id", studentId)
                .eq("deleted", false)
                .orderBy("createdAt desc")
                .findList();
    }

    public List<Offer> byStudentAndStatus(Long studentId, OfferStatus status) {
        return finder.query().where()
                .eq("student.id", studentId)
                .eq("status", status)
                .eq("deleted", false)
                .findList();
    }

    public List<Offer> byDrive(Long driveId) {
        return finder.query().where()
                .eq("drive.id", driveId)
                .eq("deleted", false)
                .findList();
    }

    /** All pending offers for a student — for placement policy enforcement */
    public List<Offer> pendingByStudent(Long studentId) {
        return finder.query().where()
                .eq("student.id", studentId)
                .eq("status", OfferStatus.PENDING)
                .eq("deleted", false)
                .findList();
    }

    /** All accepted offers for a student — for dream CTC / block-after-accept checks */
    public List<Offer> acceptedByStudent(Long studentId) {
        return finder.query().where()
                .eq("student.id", studentId)
                .eq("status", OfferStatus.ACCEPTED)
                .eq("deleted", false)
                .findList();
    }

    /** Count active (non-declined, non-expired) offers for max simultaneous check */
    public int countActiveByStudent(Long studentId) {
        return finder.query().where()
                .eq("student.id", studentId)
                .in("status", OfferStatus.PENDING, OfferStatus.ACCEPTED)
                .eq("deleted", false)
                .findCount();
    }

    public List<Offer> findRecent(int limit) {
        return finder.query()
                .fetch("student")
                .fetch("student.user")
                .fetch("drive")
                .fetch("drive.companyCollege")
                .fetch("drive.companyCollege.company")
                .where().eq("deleted", false)
                .orderBy("createdAt desc")
                .setMaxRows(limit)
                .findList();
    }

    public ExpressionList<Offer> where() {
        return finder.query().where().eq("deleted", false);
    }
}

package models.repos;

import helpers.sql.SqlFinder;
import io.ebean.ExpressionList;
import models.enums.SubscriptionTier;
import models.sql.Subscription;

import java.sql.Timestamp;
import java.util.List;

public enum SubscriptionRepository {
    INSTANCE;

    private final SqlFinder<Long, Subscription> finder = new SqlFinder<>(Subscription.class);

    public Subscription byId(Long id) {
        return finder.query().where().eq("id", id).eq("deleted", false).findOne();
    }

    /**
     * Get active premium subscription for a student.
     */
    public Subscription activeByStudent(Long studentId) {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        return finder.query().where()
                .eq("student.id", studentId)
                .eq("tier", SubscriptionTier.PREMIUM)
                .eq("active", true)
                .eq("deleted", false)
                .or()
                    .isNull("endDate")
                    .gt("endDate", now)
                .endOr()
                .setMaxRows(1)
                .findOne();
    }

    /**
     * Get active premium subscription for a college (covers all students).
     */
    public Subscription activeByCollege(Long collegeId) {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        return finder.query().where()
                .eq("college.id", collegeId)
                .eq("tier", SubscriptionTier.PREMIUM)
                .eq("active", true)
                .eq("deleted", false)
                .or()
                    .isNull("endDate")
                    .gt("endDate", now)
                .endOr()
                .setMaxRows(1)
                .findOne();
    }

    /**
     * List all subscriptions for a student.
     */
    public List<Subscription> byStudent(Long studentId) {
        return finder.query().where()
                .eq("student.id", studentId)
                .eq("deleted", false)
                .orderBy("createdAt desc")
                .findList();
    }

    /**
     * List all subscriptions for a college.
     */
    public List<Subscription> byCollege(Long collegeId) {
        return finder.query().where()
                .eq("college.id", collegeId)
                .eq("deleted", false)
                .orderBy("createdAt desc")
                .findList();
    }

    public List<Subscription> findRecent(int limit) {
        return finder.query()
                .fetch("student")
                .fetch("student.user")
                .fetch("college")
                .where().eq("deleted", false)
                .orderBy("createdAt desc")
                .setMaxRows(limit)
                .findList();
    }

    public ExpressionList<Subscription> where() {
        return finder.query().where().eq("deleted", false);
    }
}

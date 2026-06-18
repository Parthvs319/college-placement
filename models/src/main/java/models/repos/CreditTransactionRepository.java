package models.repos;

import helpers.sql.SqlFinder;
import io.ebean.ExpressionList;
import models.enums.CreditTransactionType;
import models.sql.CreditTransaction;

import java.util.List;

public enum CreditTransactionRepository {
    INSTANCE;

    private final SqlFinder<Long, CreditTransaction> finder = new SqlFinder<>(CreditTransaction.class);

    public CreditTransaction byId(Long id) {
        return finder.query().where().eq("id", id).eq("deleted", false).findOne();
    }

    public List<CreditTransaction> bySubscription(Long subscriptionId) {
        return finder.query().where()
                .eq("subscription.id", subscriptionId)
                .eq("deleted", false)
                .orderBy("createdAt desc")
                .findList();
    }

    public List<CreditTransaction> bySubscription(Long subscriptionId, int limit) {
        return finder.query().where()
                .eq("subscription.id", subscriptionId)
                .eq("deleted", false)
                .orderBy("createdAt desc")
                .setMaxRows(limit)
                .findList();
    }

    public List<CreditTransaction> byStudent(Long studentId) {
        return finder.query().where()
                .eq("student.id", studentId)
                .eq("deleted", false)
                .orderBy("createdAt desc")
                .findList();
    }

    public List<CreditTransaction> byCollege(Long collegeId) {
        return finder.query().where()
                .eq("college.id", collegeId)
                .eq("deleted", false)
                .orderBy("createdAt desc")
                .findList();
    }

    public List<CreditTransaction> findRecent(int limit) {
        return finder.query()
                .fetch("subscription")
                .fetch("student")
                .fetch("student.user")
                .fetch("college")
                .where().eq("deleted", false)
                .orderBy("createdAt desc")
                .setMaxRows(limit)
                .findList();
    }

    /**
     * Count total credits used by a student (sum of negative amounts).
     */
    public int totalUsedByStudent(Long studentId) {
        List<CreditTransaction> txns = finder.query().where()
                .eq("student.id", studentId)
                .lt("amount", 0)
                .eq("deleted", false)
                .findList();
        return txns.stream().mapToInt(t -> Math.abs(t.amount)).sum();
    }

    public ExpressionList<CreditTransaction> where() {
        return finder.query().where().eq("deleted", false);
    }
}

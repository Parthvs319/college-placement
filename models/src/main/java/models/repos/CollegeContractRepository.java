package models.repos;

import helpers.sql.SqlFinder;
import models.sql.CollegeContract;

import java.util.List;

public enum CollegeContractRepository {
    INSTANCE;

    private final SqlFinder<Long, CollegeContract> finder = new SqlFinder<>(CollegeContract.class);

    public CollegeContract byId(Long id) {
        return finder.query().where().eq("id", id).eq("deleted", false).findOne();
    }

    public List<CollegeContract> byCollege(Long collegeId) {
        return finder.query().where()
                .eq("college.id", collegeId)
                .eq("deleted", false)
                .orderBy("createdAt desc")
                .findList();
    }

    /** Returns the most recent active contract for a college, or null */
    public CollegeContract latestActive(Long collegeId) {
        return finder.query().where()
                .eq("college.id", collegeId)
                .eq("status", "ACTIVE")
                .eq("deleted", false)
                .orderBy("createdAt desc")
                .setMaxRows(1)
                .findOne();
    }

    /** Returns all active contracts expiring between two ISO date strings (inclusive), ordered by validTo asc */
    public List<CollegeContract> expiringBetween(String fromDate, String toDate) {
        return finder.query().where()
                .eq("status", "ACTIVE")
                .eq("deleted", false)
                .ge("validTo", fromDate)
                .le("validTo", toDate)
                .orderBy("validTo asc")
                .findList();
    }

    /**
     * Returns all ACTIVE PAID contracts that have a validFrom set —
     * caller is responsible for filtering by invoice cycle window.
     */
    public List<CollegeContract> findActiveForInvoiceDue() {
        return finder.query().where()
                .eq("status", "ACTIVE")
                .eq("contractType", "PAID")
                .eq("deleted", false)
                .isNotNull("validFrom")
                .findList();
    }

    /** Count all non-deleted contracts for a college — used to generate sequential contract numbers */
    public int countByCollege(Long collegeId) {
        return finder.query().where()
                .eq("college.id", collegeId)
                .eq("deleted", false)
                .findCount();
    }

    /** Returns the most recent contract regardless of status */
    public CollegeContract latest(Long collegeId) {
        return finder.query().where()
                .eq("college.id", collegeId)
                .eq("deleted", false)
                .orderBy("createdAt desc")
                .setMaxRows(1)
                .findOne();
    }
}

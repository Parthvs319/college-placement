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

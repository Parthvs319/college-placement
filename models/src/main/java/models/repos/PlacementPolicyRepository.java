package models.repos;

import helpers.sql.SqlFinder;
import io.ebean.ExpressionList;
import models.sql.PlacementPolicy;

import java.util.List;

public enum PlacementPolicyRepository {
    INSTANCE;

    private final SqlFinder<Long, PlacementPolicy> finder = new SqlFinder<>(PlacementPolicy.class);

    public PlacementPolicy byId(Long id) {
        return finder.query().where().eq("id", id).eq("deleted", false).findOne();
    }

    public PlacementPolicy byCollegeAndYear(Long collegeId, int academicYear) {
        return finder.query().where()
                .eq("college.id", collegeId)
                .eq("academicYear", academicYear)
                .eq("deleted", false)
                .findOne();
    }

    /** Get the latest policy for a college (highest academic year) */
    public PlacementPolicy latestByCollege(Long collegeId) {
        return finder.query().where()
                .eq("college.id", collegeId)
                .eq("deleted", false)
                .orderBy("academicYear desc")
                .setMaxRows(1)
                .findOne();
    }

    public List<PlacementPolicy> byCollege(Long collegeId) {
        return finder.query().where()
                .eq("college.id", collegeId)
                .eq("deleted", false)
                .orderBy("academicYear desc")
                .findList();
    }

    public ExpressionList<PlacementPolicy> where() {
        return finder.query().where().eq("deleted", false);
    }
}

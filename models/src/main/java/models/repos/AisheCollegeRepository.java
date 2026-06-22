package models.repos;

import helpers.sql.SqlFinder;
import models.sql.AisheCollege;

import java.util.List;

public enum AisheCollegeRepository {
    INSTANCE;

    private final SqlFinder<Long, AisheCollege> finder = new SqlFinder<>(AisheCollege.class);

    /**
     * Full-text search on college name, optionally filtered by state.
     * Returns at most {@code limit} results ordered by name.
     */
    public List<AisheCollege> search(String query, String state, int limit) {
        var q = finder.query().where()
                .like("name", "%" + query + "%");
        if (state != null && !state.isBlank()) {
            q = q.eq("state", state);
        }
        return q.orderBy("name asc")
                .setMaxRows(limit)
                .findList();
    }

    /**
     * Returns all records with only the 4 fields needed for client-side fuzzy search.
     * Fetches aisheCode, name, state, district — skips heavy columns.
     */
    public List<AisheCollege> findAllSlim() {
        return finder.query()
                .select("aisheCode, name, state, district")
                .orderBy("name asc")
                .findList();
    }

    /** Exact match by AISHE code. */
    public AisheCollege findByAisheCode(String code) {
        return finder.query().where().eq("aisheCode", code).findOne();
    }

    /** Exact match by college name — used for onboarding validation. */
    public AisheCollege findByName(String name) {
        return finder.query().where().eq("name", name).findOne();
    }
}

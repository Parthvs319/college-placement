package models.repos;

import helpers.sql.SqlFinder;
import io.ebean.ExpressionList;
import models.sql.College;

import java.util.List;

public enum CollegeRepository {
    INSTANCE;

    private final SqlFinder<Long, College> finder = new SqlFinder<>(College.class);

    public College byId(Long id) {
        return finder.query().where().eq("id", id).eq("deleted", false).findOne();
    }

    public College byCode(String code) {
        return finder.query().where().eq("code", code).eq("deleted", false).findOne();
    }
    public College byEmail(String contactEmail) {
        return finder.query().where().eq("contact_email", contactEmail).eq("deleted", false).findOne();
    }

    public List<College> findAll() {
        return finder.query().where().eq("deleted", false).findList();
    }

    public List<College> findActive() {
        return finder.query().where().eq("active", true).eq("deleted", false).findList();
    }

    public ExpressionList<College> where() {
        return finder.query().where().eq("deleted", false);
    }
}

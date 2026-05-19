package models.repos;

import helpers.sql.SqlFinder;
import io.ebean.ExpressionList;
import models.sql.Company;

import java.util.List;

public enum CompanyRepository {
    INSTANCE;

    private final SqlFinder<Long, Company> finder = new SqlFinder<>(Company.class);

    public Company byId(Long id) {
        return finder.query().where().eq("id", id).eq("deleted", false).findOne();
    }

    public Company byName(String name) {
        return finder.query().where().eq("name", name).eq("deleted", false).findOne();
    }

    public List<Company> findAll() {
        return finder.query().where().eq("deleted", false).findList();
    }

    public ExpressionList<Company> where() {
        return finder.query().where().eq("deleted", false);
    }
}

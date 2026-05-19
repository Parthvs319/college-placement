package models.repos;

import helpers.sql.SqlFinder;
import io.ebean.ExpressionList;
import models.sql.CompanyCollege;

import java.util.List;

public enum CompanyCollegeRepository {
    INSTANCE;

    private final SqlFinder<Long, CompanyCollege> finder = new SqlFinder<>(CompanyCollege.class);

    public CompanyCollege byId(Long id) {
        return finder.query().where().eq("id", id).eq("deleted", false).findOne();
    }

    public CompanyCollege byCompanyAndCollege(Long companyId, Long collegeId) {
        return finder.query().where()
                .eq("company.id", companyId)
                .eq("college.id", collegeId)
                .eq("deleted", false)
                .findOne();
    }

    public List<CompanyCollege> byCollege(Long collegeId) {
        return finder.query().where()
                .eq("college.id", collegeId)
                .eq("active", true)
                .eq("deleted", false)
                .findList();
    }

    public List<CompanyCollege> byCompany(Long companyId) {
        return finder.query().where()
                .eq("company.id", companyId)
                .eq("active", true)
                .eq("deleted", false)
                .findList();
    }

    public List<CompanyCollege> byManagedUser(Long userId) {
        return finder.query().where()
                .eq("managedByUser.id", userId)
                .eq("deleted", false)
                .findList();
    }

    public ExpressionList<CompanyCollege> where() {
        return finder.query().where().eq("deleted", false);
    }
}

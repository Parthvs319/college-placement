package models.repos;

import helpers.sql.SqlFinder;
import io.ebean.ExpressionList;
import models.enums.RoundType;
import models.sql.PYQ;

import java.util.List;

public enum PYQRepository {
    INSTANCE;

    private final SqlFinder<Long, PYQ> finder = new SqlFinder<>(PYQ.class);

    public PYQ byId(Long id) {
        return finder.query().where().eq("id", id).eq("deleted", false).findOne();
    }

    public List<PYQ> byCompany(Long companyId) {
        return finder.query().where()
                .eq("company.id", companyId)
                .eq("deleted", false)
                .orderBy("year desc, upvotes desc")
                .findList();
    }

    public List<PYQ> byCompanyAndRoundType(Long companyId, RoundType roundType) {
        return finder.query().where()
                .eq("company.id", companyId)
                .eq("roundType", roundType)
                .eq("deleted", false)
                .orderBy("year desc, upvotes desc")
                .findList();
    }

    public List<PYQ> byCollege(Long collegeId) {
        return finder.query().where()
                .eq("college.id", collegeId)
                .eq("deleted", false)
                .orderBy("year desc, upvotes desc")
                .findList();
    }

    public List<PYQ> byCompanyAndYear(Long companyId, int year) {
        return finder.query().where()
                .eq("company.id", companyId)
                .eq("year", year)
                .eq("deleted", false)
                .orderBy("upvotes desc")
                .findList();
    }

    /** Search by difficulty for a company */
    public List<PYQ> byCompanyAndDifficulty(Long companyId, String difficulty) {
        return finder.query().where()
                .eq("company.id", companyId)
                .eq("difficulty", difficulty)
                .eq("deleted", false)
                .orderBy("year desc")
                .findList();
    }

    /** Top upvoted PYQs across all companies (for explore page) */
    public List<PYQ> topUpvoted(int limit) {
        return finder.query().where()
                .eq("deleted", false)
                .orderBy("upvotes desc")
                .setMaxRows(limit)
                .findList();
    }

    public ExpressionList<PYQ> where() {
        return finder.query().where().eq("deleted", false);
    }
}

package models.repos;

import helpers.sql.SqlFinder;
import io.ebean.ExpressionList;
import models.sql.Document;

import java.util.List;

public enum DocumentRepository {
    INSTANCE;

    private final SqlFinder<Long, Document> finder = new SqlFinder<>(Document.class);

    public Document byId(Long id) {
        return finder.query().where().eq("id", id).eq("deleted", false).findOne();
    }

    public List<Document> byCollege(Long collegeId) {
        return finder.query().where()
                .eq("college.id", collegeId)
                .eq("deleted", false)
                .orderBy("createdAt desc")
                .findList();
    }

    public List<Document> byCollegeAndType(Long collegeId, String type) {
        return finder.query().where()
                .eq("college.id", collegeId)
                .eq("type", type)
                .eq("deleted", false)
                .orderBy("createdAt desc")
                .findList();
    }

    public List<Document> byCompany(Long companyId) {
        return finder.query().where()
                .eq("company.id", companyId)
                .eq("deleted", false)
                .orderBy("createdAt desc")
                .findList();
    }

    public List<Document> byCollegeAndYear(Long collegeId, int academicYear) {
        return finder.query().where()
                .eq("college.id", collegeId)
                .eq("academicYear", academicYear)
                .eq("deleted", false)
                .orderBy("createdAt desc")
                .findList();
    }

    public ExpressionList<Document> where() {
        return finder.query().where().eq("deleted", false);
    }
}

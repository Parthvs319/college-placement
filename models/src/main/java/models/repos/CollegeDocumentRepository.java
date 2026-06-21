package models.repos;

import helpers.sql.SqlFinder;
import models.sql.CollegeDocument;

import java.util.List;

public enum CollegeDocumentRepository {
    INSTANCE;

    private final SqlFinder<Long, CollegeDocument> finder = new SqlFinder<>(CollegeDocument.class);

    public CollegeDocument byId(Long id) {
        return finder.query().where().eq("id", id).eq("deleted", false).findOne();
    }

    public List<CollegeDocument> byCollegeId(Long collegeId) {
        return finder.query().where()
                .eq("college.id", collegeId)
                .eq("deleted", false)
                .orderBy("documentType asc, createdAt desc")
                .findList();
    }

    public List<CollegeDocument> byCollegeIdAndType(Long collegeId, String documentType) {
        return finder.query().where()
                .eq("college.id", collegeId)
                .eq("documentType", documentType)
                .eq("deleted", false)
                .orderBy("createdAt desc")
                .findList();
    }
}

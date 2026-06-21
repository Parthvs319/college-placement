package models.repos;

import helpers.sql.SqlFinder;
import models.sql.StudentDocument;

import java.util.List;

public enum StudentDocumentRepository {
    INSTANCE;

    private final SqlFinder<Long, StudentDocument> finder = new SqlFinder<>(StudentDocument.class);

    public StudentDocument byId(Long id) {
        return finder.query().where().eq("id", id).eq("deleted", false).findOne();
    }

    public List<StudentDocument> byStudentId(Long studentId) {
        return finder.query().where()
                .eq("student.id", studentId)
                .eq("deleted", false)
                .orderBy("documentType asc, semester asc, createdAt desc")
                .findList();
    }

    public List<StudentDocument> byStudentIdAndType(Long studentId, String documentType) {
        return finder.query().where()
                .eq("student.id", studentId)
                .eq("documentType", documentType)
                .eq("deleted", false)
                .orderBy("semester asc, createdAt desc")
                .findList();
    }
}

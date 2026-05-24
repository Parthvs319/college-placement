package models.repos;

import helpers.sql.SqlFinder;
import models.sql.Resume;

import java.util.List;

public enum ResumeRepository {
    INSTANCE;

    private final SqlFinder<Long, Resume> finder = new SqlFinder<>(Resume.class);

    public Resume byId(Long id) {
        return finder.query().where().eq("id", id).eq("deleted", false).findOne();
    }

    public List<Resume> byStudent(Long studentId) {
        return finder.query().where()
                .eq("student.id", studentId)
                .eq("deleted", false)
                .orderBy("primary desc, createdAt desc")
                .findList();
    }

    public Resume primaryByStudent(Long studentId) {
        return finder.query().where()
                .eq("student.id", studentId)
                .eq("primary", true)
                .eq("deleted", false)
                .findOne();
    }

    public int countByStudent(Long studentId) {
        return finder.query().where()
                .eq("student.id", studentId)
                .eq("deleted", false)
                .findCount();
    }

    /**
     * Unset primary flag on all resumes for a student.
     * Called before setting a new primary.
     */
    public void clearPrimary(Long studentId) {
        List<Resume> resumes = finder.query().where()
                .eq("student.id", studentId)
                .eq("primary", true)
                .eq("deleted", false)
                .findList();
        for (Resume r : resumes) {
            r.primary = false;
            r.update();
        }
    }
}

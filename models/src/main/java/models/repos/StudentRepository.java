package models.repos;

import helpers.sql.SqlFinder;
import io.ebean.ExpressionList;
import models.sql.Student;

import java.math.BigDecimal;
import java.util.List;

public enum StudentRepository {
    INSTANCE;

    private final SqlFinder<Long, Student> finder = new SqlFinder<>(Student.class);

    public Student byId(Long id) {
        return finder.query().where().eq("id", id).eq("deleted", false).findOne();
    }

    public Student byUserId(Long userId) {
        return finder.query().where().eq("user.id", userId).eq("deleted", false).findOne();
    }

    public Student byEnrollment(String enrollmentNumber, Long collegeId) {
        return finder.query().where()
                .eq("enrollmentNumber", enrollmentNumber)
                .eq("college.id", collegeId)
                .eq("deleted", false)
                .findOne();
    }

    public List<Student> byCollege(Long collegeId) {
        return finder.query().where()
                .eq("college.id", collegeId)
                .eq("deleted", false)
                .findList();
    }

    public List<Student> findEligible(Long collegeId, BigDecimal minCgpa, int maxBacklogs,
                                       List<String> departments, int passingYear) {
        ExpressionList<Student> expr = finder.query().where()
                .eq("college.id", collegeId)
                .ge("cgpa", minCgpa)
                .le("activeBacklogs", maxBacklogs)
                .eq("passingYear", passingYear)
                .eq("optedOut", false)
                .eq("deleted", false);

        if (departments != null && !departments.isEmpty()) {
            expr.in("department", departments);
        }

        return expr.findList();
    }

    public List<Student> findPlaced(Long collegeId) {
        return finder.query().where()
                .eq("college.id", collegeId)
                .eq("placed", true)
                .eq("deleted", false)
                .findList();
    }

    public List<Student> findUnplaced(Long collegeId) {
        return finder.query().where()
                .eq("college.id", collegeId)
                .eq("placed", false)
                .eq("optedOut", false)
                .eq("deleted", false)
                .findList();
    }

    public ExpressionList<Student> where() {
        return finder.query().where().eq("deleted", false);
    }

    /** where() with eager-fetched user and college relationships */
    public ExpressionList<Student> whereWithFetch() {
        return finder.query()
                .fetch("user")
                .fetch("college")
                .where().eq("deleted", false);
    }
}

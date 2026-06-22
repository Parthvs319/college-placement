package models.repos;

import helpers.sql.SqlFinder;
import io.ebean.ExpressionList;
import models.enums.DriveStatus;
import models.enums.EmploymentType;
import models.sql.Drive;

import java.util.List;

public enum DriveRepository {
    INSTANCE;

    private final SqlFinder<Long, Drive> finder = new SqlFinder<>(Drive.class);

    public Drive byId(Long id) {
        return finder.query().where().eq("id", id).eq("deleted", false).findOne();
    }

    public List<Drive> byCollege(Long collegeId) {
        return finder.query().where()
                .eq("companyCollege.college.id", collegeId)
                .eq("deleted", false)
                .orderBy("driveDate desc")
                .findList();
    }

    public List<Drive> byCollegeAndYear(Long collegeId, int academicYear) {
        return finder.query().where()
                .eq("companyCollege.college.id", collegeId)
                .eq("academicYear", academicYear)
                .eq("deleted", false)
                .orderBy("driveDate asc")
                .findList();
    }

    public List<Drive> byCompanyCollege(Long companyCollegeId) {
        return finder.query().where()
                .eq("companyCollege.id", companyCollegeId)
                .eq("deleted", false)
                .orderBy("driveDate desc")
                .findList();
    }

    public List<Drive> byStatus(Long collegeId, DriveStatus status) {
        return finder.query().where()
                .eq("companyCollege.college.id", collegeId)
                .eq("status", status)
                .eq("deleted", false)
                .findList();
    }

    public List<Drive> upcoming(Long collegeId) {
        return finder.query().where()
                .eq("companyCollege.college.id", collegeId)
                .in("status", DriveStatus.UPCOMING, DriveStatus.REGISTRATION_OPEN)
                .eq("deleted", false)
                .orderBy("driveDate asc")
                .findList();
    }

    public List<Drive> byCompany(Long companyId) {
        return finder.query().where()
                .eq("companyCollege.company.id", companyId)
                .eq("deleted", false)
                .orderBy("driveDate desc")
                .findList();
    }

    public List<Drive> findRecent(int limit) {
        return finder.query()
                .fetch("companyCollege")
                .fetch("companyCollege.company")
                .fetch("companyCollege.college")
                .where().eq("deleted", false)
                .orderBy("createdAt desc")
                .setMaxRows(limit)
                .findList();
    }

    public int countByCollege(Long collegeId) {
        return finder.query().where()
                .eq("companyCollege.college.id", collegeId)
                .eq("deleted", false)
                .findCount();
    }

    public int countByCollegeAndType(Long collegeId, EmploymentType type) {
        return finder.query().where()
                .eq("companyCollege.college.id", collegeId)
                .eq("employmentType", type)
                .eq("deleted", false)
                .findCount();
    }

    public ExpressionList<Drive> where() {
        return finder.query().where().eq("deleted", false);
    }
}

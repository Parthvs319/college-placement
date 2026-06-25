package models.repos;

import helpers.sql.SqlFinder;
import io.ebean.ExpressionList;
import models.sql.PortalPermission;

import java.util.List;

public enum PortalPermissionRepository {
    INSTANCE;

    private final SqlFinder<Long, PortalPermission> finder = new SqlFinder<>(PortalPermission.class);

    private ExpressionList<PortalPermission> where() {
        return finder.query().where();
    }

    /** Fetch permissions for a sub-TPO in a specific college. */
    public PortalPermission byUserAndCollege(Long userId, Long collegeId) {
        return where()
                .eq("user.id", userId)
                .eq("college.id", collegeId)
                .eq("deleted", false)
                .findOne();
    }

    /** Fetch permissions for a sub-HR in a specific company. */
    public PortalPermission byUserAndCompany(Long userId, Long companyId) {
        return where()
                .eq("user.id", userId)
                .eq("company.id", companyId)
                .eq("deleted", false)
                .findOne();
    }

    /** All team members for a college (non-primary users with permissions). */
    public List<PortalPermission> byCollege(Long collegeId) {
        return where()
                .eq("college.id", collegeId)
                .eq("deleted", false)
                .findList();
    }

    /** All team members for a company (non-primary users with permissions). */
    public List<PortalPermission> byCompany(Long companyId) {
        return where()
                .eq("company.id", companyId)
                .eq("deleted", false)
                .findList();
    }
}

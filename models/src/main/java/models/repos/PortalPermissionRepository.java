package models.repos;

import helpers.blueprint.repos.BaseRepository;
import models.sql.PortalPermission;

public enum PortalPermissionRepository implements BaseRepository<PortalPermission> {
    INSTANCE;

    @Override
    public Class<PortalPermission> getModelClass() {
        return PortalPermission.class;
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
    public java.util.List<PortalPermission> byCollege(Long collegeId) {
        return where()
                .eq("college.id", collegeId)
                .eq("deleted", false)
                .findList();
    }

    /** All team members for a company (non-primary users with permissions). */
    public java.util.List<PortalPermission> byCompany(Long companyId) {
        return where()
                .eq("company.id", companyId)
                .eq("deleted", false)
                .findList();
    }
}

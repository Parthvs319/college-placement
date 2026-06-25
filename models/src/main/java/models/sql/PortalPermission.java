package models.sql;

import helpers.blueprint.models.BaseModel;
import io.ebean.annotation.DbJsonB;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.util.Map;

/**
 * Stores module-level read/write permissions for sub-TPOs (college) and sub-HRs (company).
 * Primary users (isPrimary=true on User) bypass this table entirely.
 *
 * permissions JSON shape:
 *   { "drives": "write", "students": "read", "companies": "none" }
 * Valid values: "none", "read", "write"
 */
@EqualsAndHashCode(callSuper = true)
@Entity
@Data
@Table(name = "portal_permissions")
public class PortalPermission extends BaseModel {

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    public User user;

    @ManyToOne
    @JoinColumn(name = "college_id")
    public College college;

    @ManyToOne
    @JoinColumn(name = "company_id")
    public Company company;

    /** Map of module -> access level ("none" | "read" | "write") */
    @DbJsonB
    public Map<String, String> permissions;

    @ManyToOne
    @JoinColumn(name = "created_by_user_id", nullable = false)
    public User createdBy;

    // ── Helpers ───────────────────────────────────────────────

    /** Returns true if the user has at least "read" on the given module. */
    public boolean canRead(String module) {
        String level = permissions == null ? null : permissions.get(module);
        return "read".equals(level) || "write".equals(level);
    }

    /** Returns true if the user has "write" on the given module. */
    public boolean canWrite(String module) {
        String level = permissions == null ? null : permissions.get(module);
        return "write".equals(level);
    }
}

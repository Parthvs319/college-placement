package company;

import helpers.annotations.CompanyRole;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.company.CompanyAccessMiddleware;
import models.body.CompanyLoginRequest;
import models.repos.PortalPermissionRepository;
import models.repos.UserRepository;
import models.sql.PortalPermission;
import models.sql.User;

import java.util.*;

/**
 * PUT /company/team/:userId
 * Update a sub-HR's module permissions.
 * Body: { permissions: { drives?: string, applicants?: string } }
 */
@CompanyRole
public enum UpdateCompanyTeamMemberController implements BaseController {

    INSTANCE;

    private static final Set<String> VALID_MODULES = Set.of("drives", "applicants");
    private static final Set<String> VALID_LEVELS  = Set.of("none", "read", "write");

    @Override
    public void handle(RoutingContext event) {
        CompanyAccessMiddleware.INSTANCE.with(event, new ArrayList<>(), this.getClass())
                .map(this::map)
                .subscribe(
                        o -> ResponseUtils.INSTANCE.writeJsonResponse(event, o),
                        error -> ResponseUtils.INSTANCE.handleError(event, error)
                );
    }

    private Object map(CompanyLoginRequest request) {
        if (!request.getUser().isPrimary) throw new RoutingError(403, "Only primary HR can update team members");
        if (request.getCompany() == null) throw new RoutingError(404, "No company linked to your account");

        Long targetUserId = Long.parseLong(request.getRoutingContext().pathParam("userId"));
        Long companyId    = request.getCompany().getId();

        User target = UserRepository.INSTANCE.byId(targetUserId);
        if (target == null) throw new RoutingError(404, "User not found");
        if (target.isPrimary) throw new RoutingError(400, "Cannot modify permissions of a primary HR");

        @SuppressWarnings("unchecked")
        Map<String, String> permMap = (Map<String, String>) request.getRequest().get("permissions");
        if (permMap == null || permMap.isEmpty()) throw new RoutingError("permissions map is required");
        for (Map.Entry<String, String> e : permMap.entrySet()) {
            if (!VALID_MODULES.contains(e.getKey())) throw new RoutingError("Unknown module: " + e.getKey());
            if (!VALID_LEVELS.contains(e.getValue())) throw new RoutingError("Invalid level for " + e.getKey());
        }

        PortalPermission perm = PortalPermissionRepository.INSTANCE.byUserAndCompany(targetUserId, companyId);
        if (perm == null) throw new RoutingError(404, "Permission record not found for this user");

        perm.permissions = permMap;
        perm.update();

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("message",     "Permissions updated");
        res.put("userId",      targetUserId);
        res.put("permissions", permMap);
        return res;
    }
}

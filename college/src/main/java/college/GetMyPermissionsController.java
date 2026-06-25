package college;

import helpers.annotations.CollegeRole;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.college.CollegeAccessMiddleware;
import models.body.CollegeLoginRequest;
import models.repos.PortalPermissionRepository;
import models.sql.PortalPermission;

import java.util.*;

/**
 * GET /college/me/permissions
 *
 * Returns the current user's isPrimary flag and module permissions.
 * Primary users return null permissions (full access implied).
 * Sub-users return their portal_permissions JSON.
 *
 * Used by the frontend on mount to populate the permission store.
 */
@CollegeRole
public enum GetMyPermissionsController implements BaseController {
    INSTANCE;

    @Override
    public void handle(RoutingContext event) {
        CollegeAccessMiddleware.INSTANCE.with(event, new ArrayList<>(), this.getClass())
                .map(this::map)
                .subscribe(
                        o -> ResponseUtils.INSTANCE.writeJsonResponse(event, o),
                        error -> ResponseUtils.INSTANCE.handleError(event, error)
                );
    }

    private Object map(CollegeLoginRequest req) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("isPrimary", req.getUser().isPrimary);

        if (req.getUser().isPrimary) {
            response.put("permissions", null); // primary = full access
        } else {
            PortalPermission perm = PortalPermissionRepository.INSTANCE
                    .byUserAndCollege(req.getUser().getId(), req.getCollege().getId());
            response.put("permissions", perm != null ? perm.permissions : new HashMap<>());
        }

        return response;
    }
}

package college.team;

import helpers.annotations.CollegeRole;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.college.CollegeAccessMiddleware;
import models.body.CollegeLoginRequest;
import models.repos.PortalPermissionRepository;
import models.repos.UserRepository;
import models.sql.PortalPermission;
import models.sql.User;

import java.util.*;

@CollegeRole(request = {"userId:path"})
public enum UpdateTeamMemberController implements BaseController {
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

    @SuppressWarnings("unchecked")
    private Object map(CollegeLoginRequest req) {
        if (!req.getUser().isPrimary) {
            throw new RoutingError(403, "Only the primary account holder can update team permissions");
        }

        String userIdStr = (String) req.getRequest().get("userId");
        Long targetUserId;
        try {
            targetUserId = Long.parseLong(userIdStr);
        } catch (NumberFormatException e) {
            throw new RoutingError("Invalid userId");
        }

        Object permsObj = req.getRequest().get("permissions");
        if (permsObj == null) throw new RoutingError("permissions is required");
        Map<String, String> permissions = (Map<String, String>) permsObj;

        // Validate
        for (Map.Entry<String, String> entry : permissions.entrySet()) {
            String val = entry.getValue();
            if (!"none".equals(val) && !"read".equals(val) && !"write".equals(val)) {
                throw new RoutingError("Invalid permission value '" + val + "'");
            }
        }

        // Cannot modify primary user
        User target = UserRepository.INSTANCE.byId(targetUserId);
        if (target == null || target.isDeleted()) throw new RoutingError(404, "User not found");
        if (target.isPrimary) throw new RoutingError(400, "Cannot change permissions of the primary account holder");

        PortalPermission perm = PortalPermissionRepository.INSTANCE
                .byUserAndCollege(targetUserId, req.getCollege().getId());
        if (perm == null) throw new RoutingError(404, "Team member not found for this college");

        perm.permissions = permissions;
        perm.save();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "Permissions updated");
        response.put("userId", targetUserId);
        response.put("permissions", permissions);
        return response;
    }
}

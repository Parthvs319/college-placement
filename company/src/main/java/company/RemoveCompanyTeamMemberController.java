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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * DELETE /company/team/:userId
 * Primary HR removes a sub-HR. Soft-deletes their permission + deactivates user.
 */
@CompanyRole
public enum RemoveCompanyTeamMemberController implements BaseController {

    INSTANCE;

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
        if (!request.getUser().isPrimary) throw new RoutingError(403, "Only primary HR can remove team members");
        if (request.getCompany() == null) throw new RoutingError(404, "No company linked to your account");

        Long targetUserId = Long.parseLong(request.getRoutingContext().pathParam("userId"));
        Long companyId    = request.getCompany().getId();

        if (targetUserId.equals(request.getUser().getId())) {
            throw new RoutingError(400, "Cannot remove yourself");
        }

        User target = UserRepository.INSTANCE.byId(targetUserId);
        if (target == null) throw new RoutingError(404, "User not found");
        if (target.isPrimary) throw new RoutingError(400, "Cannot remove a primary HR");

        PortalPermission perm = PortalPermissionRepository.INSTANCE.byUserAndCompany(targetUserId, companyId);
        if (perm != null) {
            perm.setDeleted(true);
            perm.save();
        }

        target.active = false;
        target.save();

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("message", "Team member removed");
        res.put("userId",  targetUserId);
        return res;
    }
}

package company;

import helpers.annotations.CompanyRole;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.company.CompanyAccessMiddleware;
import models.body.CompanyLoginRequest;
import models.enums.UserType;
import models.repos.PortalPermissionRepository;
import models.repos.UserRepository;
import models.sql.PortalPermission;
import models.sql.User;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * GET /company/team
 * Primary HR sees all team members + their permissions.
 */
@CompanyRole
public enum ListCompanyTeamController implements BaseController {

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
        if (!request.getUser().isPrimary) throw new RoutingError(403, "Only primary HR can view team");
        if (request.getCompany() == null) throw new RoutingError(404, "No company linked to your account");

        Long companyId = request.getCompany().getId();

        // Primary user
        User primary = request.getUser();
        List<Map<String, Object>> members = new ArrayList<>();

        Map<String, Object> primaryMap = new LinkedHashMap<>();
        primaryMap.put("userId",      primary.getId());
        primaryMap.put("name",        primary.name);
        primaryMap.put("email",       primary.email);
        primaryMap.put("isPrimary",   true);
        primaryMap.put("verified",    primary.verified);
        primaryMap.put("active",      primary.active);
        primaryMap.put("permissions", null);
        members.add(primaryMap);

        // Sub-HRs via portal_permissions
        List<PortalPermission> perms = PortalPermissionRepository.INSTANCE.byCompany(companyId);
        for (PortalPermission p : perms) {
            User u = p.user;
            if (u == null || u.getId().equals(primary.getId())) continue;

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("userId",      u.getId());
            m.put("name",        u.name);
            m.put("email",       u.email);
            m.put("isPrimary",   false);
            m.put("verified",    u.verified);
            m.put("active",      u.active);
            m.put("permissions", p.permissions);
            members.add(m);
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("members", members);
        res.put("total",   members.size());
        return res;
    }
}

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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@CollegeRole
public enum ListCollegeTeamController implements BaseController {
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
        if (!req.getUser().isPrimary) {
            throw new RoutingError(403, "Only the primary account holder can manage team members");
        }

        Long collegeId = req.getCollege().getId();

        // Primary user entry
        User primaryUser = req.getUser();
        List<Map<String, Object>> members = new ArrayList<>();

        // The caller is primary — include self
        Map<String, Object> primary = new LinkedHashMap<>();
        primary.put("userId", primaryUser.getId());
        primary.put("name", primaryUser.name);
        primary.put("email", primaryUser.email);
        primary.put("isPrimary", true);
        primary.put("verified", primaryUser.verified);
        primary.put("active", primaryUser.active);
        primary.put("permissions", null); // primary has full access
        members.add(primary);

        // Sub-team members
        List<PortalPermission> perms = PortalPermissionRepository.INSTANCE.byCollege(collegeId);
        for (PortalPermission perm : perms) {
            User u = perm.user;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("userId", u.getId());
            m.put("name", u.name);
            m.put("email", u.email);
            m.put("isPrimary", false);
            m.put("verified", u.verified);
            m.put("active", u.active);
            m.put("permissions", perm.permissions);
            members.add(m);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("members", members);
        response.put("total", members.size());
        return response;
    }
}

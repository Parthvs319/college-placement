package company;

import helpers.annotations.CompanyRole;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.company.CompanyAccessMiddleware;
import models.body.CompanyLoginRequest;
import models.repos.PortalPermissionRepository;
import models.sql.PortalPermission;
import models.sql.User;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * GET /company/me/permissions
 * Returns { isPrimary, permissions } for the logged-in company HR.
 * Primary users get permissions: null (they have full access).
 */
@CompanyRole
public enum GetCompanyMyPermissionsController implements BaseController {

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
        User user = request.getUser();

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("isPrimary", user.isPrimary);

        if (user.isPrimary || request.getCompany() == null) {
            res.put("permissions", null);
        } else {
            PortalPermission perm = PortalPermissionRepository.INSTANCE
                    .byUserAndCompany(user.getId(), request.getCompany().getId());
            res.put("permissions", perm != null ? perm.permissions : null);
        }
        return res;
    }
}

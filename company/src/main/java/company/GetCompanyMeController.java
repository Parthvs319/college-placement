package company;

import helpers.annotations.CompanyRole;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.company.CompanyAccessMiddleware;
import models.body.CompanyLoginRequest;
import models.sql.Company;
import models.sql.User;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * GET /company/me
 * Returns the logged-in HR user's profile + their company details.
 */
@CompanyRole
public enum GetCompanyMeController implements BaseController {

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
        User    user    = request.getUser();
        Company company = request.getCompany();

        Map<String, Object> userMap = new LinkedHashMap<>();
        userMap.put("id",        user.getId());
        userMap.put("name",      user.name);
        userMap.put("email",     user.email);
        userMap.put("mobile",    user.mobile);
        userMap.put("avatarUrl", user.avatarUrl);
        userMap.put("isPrimary", user.isPrimary);
        userMap.put("verified",  user.verified);
        userMap.put("active",    user.active);

        Map<String, Object> companyMap = new LinkedHashMap<>();
        if (company != null) {
            companyMap.put("id",           company.getId());
            companyMap.put("code",         company.code);
            companyMap.put("name",         company.name);
            companyMap.put("industry",     company.industry);
            companyMap.put("website",      company.website);
            companyMap.put("logoUrl",      company.logoUrl);
            companyMap.put("description",  company.description);
            companyMap.put("headquarters", company.headquarters);
            companyMap.put("contactEmail", company.contactEmail);
            companyMap.put("contactPhone", company.contactPhone);
            companyMap.put("startup",      company.startup);
            companyMap.put("active",       company.active);
        }

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("user",    userMap);
        res.put("company", companyMap);
        return res;
    }
}

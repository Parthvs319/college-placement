package company;

import helpers.annotations.CompanyRole;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.company.CompanyAccessMiddleware;
import models.body.CompanyLoginRequest;
import models.sql.Company;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * PUT /company/me
 * Primary HR can update company profile fields.
 */
@CompanyRole
public enum UpdateCompanyMeController implements BaseController {

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
        if (!request.getUser().isPrimary) throw new RoutingError(403, "Only primary HR can update company profile");

        Company company = request.getCompany();
        if (company == null) throw new RoutingError(404, "No company linked to your account");

        var body = request.getRequest();
        if (body.isPresent("industry"))     company.industry     = body.get("industry").trim();
        if (body.isPresent("website"))      company.website      = body.get("website").trim();
        if (body.isPresent("logoUrl"))      company.logoUrl      = body.get("logoUrl").trim();
        if (body.isPresent("description"))  company.description  = body.get("description").trim();
        if (body.isPresent("headquarters")) company.headquarters = body.get("headquarters").trim();
        if (body.isPresent("contactPhone")) company.contactPhone = body.get("contactPhone").trim();
        if (body.isPresent("startup"))      company.startup      = Boolean.parseBoolean(body.get("startup"));
        company.update();

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("message", "Company profile updated");
        res.put("companyId", company.getId());
        return res;
    }
}

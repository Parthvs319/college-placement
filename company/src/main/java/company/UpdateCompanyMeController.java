package company;

import helpers.annotations.CompanyRole;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.company.CompanyAccessMiddleware;
import models.body.CompanyLoginRequest;
import models.sql.Company;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * PUT /company/me
 * Primary HR can update company profile fields.
 * Reads the raw JSON body directly (the middleware Request wrapper requires
 * pre-declared fields; for flexible updates we read the body ourselves).
 */
@CompanyRole
public enum UpdateCompanyMeController implements BaseController {

    INSTANCE;

    @Override
    public void handle(RoutingContext event) {
        CompanyAccessMiddleware.INSTANCE.with(event, new ArrayList<>(), this.getClass())
                .map(req -> map(req, event))
                .subscribe(
                        o -> ResponseUtils.INSTANCE.writeJsonResponse(event, o),
                        error -> ResponseUtils.INSTANCE.handleError(event, error)
                );
    }

    private Object map(CompanyLoginRequest request, RoutingContext event) {
        if (!request.getUser().isPrimary) throw new RoutingError(403, "Only primary HR can update company profile");

        Company company = request.getCompany();
        if (company == null) throw new RoutingError(404, "No company linked to your account");

        JsonObject body = event.body().asJsonObject();
        if (body == null) body = new JsonObject();

        // Basic fields
        if (body.containsKey("industry"))     company.industry     = body.getString("industry");
        if (body.containsKey("website"))      company.website      = body.getString("website");
        if (body.containsKey("logoUrl"))      company.logoUrl      = body.getString("logoUrl");
        if (body.containsKey("description"))  company.description  = body.getString("description");
        if (body.containsKey("headquarters")) company.headquarters = body.getString("headquarters");
        if (body.containsKey("contactPhone")) company.contactPhone = body.getString("contactPhone");
        if (body.containsKey("startup"))      company.startup      = Boolean.TRUE.equals(body.getBoolean("startup"));

        // Identity fields
        if (body.containsKey("companyType"))         company.companyType        = body.getString("companyType");
        if (body.containsKey("cin"))                 company.cin                = body.getString("cin");
        if (body.containsKey("gstin"))               company.gstin              = body.getString("gstin");
        if (body.containsKey("yearOfEstablishment")) company.yearOfEstablishment = body.getInteger("yearOfEstablishment");
        if (body.containsKey("employeeCount"))       company.employeeCount      = body.getString("employeeCount");
        if (body.containsKey("linkedinUrl"))         company.linkedinUrl        = body.getString("linkedinUrl");

        // Recruitment preferences (list fields)
        if (body.containsKey("typicalRoles"))         company.typicalRoles         = jsonArrayToList(body.getJsonArray("typicalRoles"));
        if (body.containsKey("preferredDepartments")) company.preferredDepartments = jsonArrayToList(body.getJsonArray("preferredDepartments"));
        if (body.containsKey("employmentTypes"))      company.employmentTypes      = jsonArrayToList(body.getJsonArray("employmentTypes"));
        if (body.containsKey("hiringCities"))         company.hiringCities         = jsonArrayToList(body.getJsonArray("hiringCities"));
        if (body.containsKey("minCgpa") && body.getValue("minCgpa") != null)
            company.minCgpa = new BigDecimal(body.getValue("minCgpa").toString());
        if (body.containsKey("minCtc") && body.getValue("minCtc") != null)
            company.minCtc = new BigDecimal(body.getValue("minCtc").toString());
        if (body.containsKey("maxCtc") && body.getValue("maxCtc") != null)
            company.maxCtc = new BigDecimal(body.getValue("maxCtc").toString());

        // HR contact extras
        if (body.containsKey("hrDesignation")) company.hrDesignation = body.getString("hrDesignation");
        if (body.containsKey("hrLinkedin"))    company.hrLinkedin    = body.getString("hrLinkedin");

        company.update();

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("message", "Company profile updated");
        res.put("companyId", company.getId());
        return res;
    }

    private static List<String> jsonArrayToList(JsonArray arr) {
        if (arr == null) return null;
        List<String> list = new ArrayList<>();
        for (int i = 0; i < arr.size(); i++) list.add(arr.getString(i));
        return list.isEmpty() ? null : list;
    }
}

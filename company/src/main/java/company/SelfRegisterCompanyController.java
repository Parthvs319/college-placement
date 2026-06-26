package company;

import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.enums.UserType;
import models.repos.CompanyRepository;
import models.repos.UserRepository;
import models.services.EmailService;
import models.sql.Company;
import models.sql.User;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Public (no auth) endpoint — a company self-registers on Applyra.
 * Creates Company(active=false) + COMPANY_HR User(active=false, verified=false).
 * Sends a notification email to support@applyra.in.
 * Superadmin must approve via POST /admin/companies/:id/approve.
 *
 * POST /company/register
 * Body: { name, industry?, website?, headquarters?, description?,
 *         hrName, hrEmail, hrPhone? }
 */
public enum SelfRegisterCompanyController implements BaseController {

    INSTANCE;

    private static final String SUPPORT_EMAIL = "support@applyra.in";

    @Override
    public void handle(RoutingContext event) {
        try {
            Object result = map(event);
            ResponseUtils.INSTANCE.writeJsonResponse(event, result);
        } catch (RoutingError e) {
            ResponseUtils.INSTANCE.handleError(event, e);
        } catch (Exception e) {
            ResponseUtils.INSTANCE.handleError(event, new RoutingError(500, e.getMessage()));
        }
    }

    private Object map(RoutingContext event) {
        JsonObject body = event.body().asJsonObject();
        if (body == null) body = new JsonObject();

        String name    = body.getString("name");
        String hrName  = body.getString("hrName");
        String hrEmail = body.getString("hrEmail");

        if (name == null || name.isBlank())       throw new RoutingError("Company name is required");
        if (hrName == null || hrName.isBlank())   throw new RoutingError("HR name is required");
        if (hrEmail == null || hrEmail.isBlank()) throw new RoutingError("HR email is required");

        final String companyName = name.trim();
        final String email       = hrEmail.trim().toLowerCase();

        // Check duplicates
        if (CompanyRepository.INSTANCE.byName(companyName) != null) {
            throw new RoutingError("A company named '" + companyName + "' already exists on Applyra");
        }
        if (UserRepository.INSTANCE.byEmail(email) != null) {
            throw new RoutingError("An account with email '" + email + "' already exists");
        }

        // Create company — inactive until approved
        Company company = new Company();
        company.name         = companyName;
        company.industry     = body.getString("industry");
        company.website      = body.getString("website");
        company.headquarters = body.getString("headquarters");
        company.description  = body.getString("description");
        company.contactEmail = email;
        company.contactPhone = body.getString("hrPhone");
        company.active       = false;   // pending Applyra approval
        company.save();

        int seq = CompanyRepository.INSTANCE.countAll();
        company.code = "CMP-SELF-" + String.format("%04d", seq);
        company.update();

        // Create HR user — inactive until approved
        User hrUser = new User();
        hrUser.email     = email;
        hrUser.name      = hrName.trim();
        hrUser.userType  = UserType.COMPANY_HR;
        hrUser.company   = company;
        hrUser.verified  = false;
        hrUser.active    = false;
        hrUser.isPrimary = true;
        hrUser.password  = ""; // no password until approval
        hrUser.save();

        // Notify Applyra team
        final Long companyId = company.getId();
        final String code    = company.code;
        EmailService.sendEmail(
                SUPPORT_EMAIL,
                "New Company Self-Registration — " + companyName,
                buildAdminNotificationHtml(companyName, code, companyId, hrName.trim(), email,
                        company.industry, company.website)
        ).subscribe(
                ok  -> System.out.println("[SelfRegister] Admin notified for " + companyName),
                err -> System.err.println("[SelfRegister] Email error: " + err.getMessage())
        );

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("message", "Registration submitted! Applyra will review and activate your account shortly. You'll receive your login credentials by email.");
        res.put("companyId",   companyId);
        res.put("companyName", companyName);
        res.put("hrEmail",     email);
        return res;
    }

    private static String buildAdminNotificationHtml(
            String companyName, String code, Long id,
            String hrName, String hrEmail,
            String industry, String website) {
        return "<!DOCTYPE html><html><body style='font-family:sans-serif;color:#1a1a1a;max-width:600px;margin:0 auto;padding:20px'>"
                + "<h2 style='color:#7c3aed'>New Company Registration — Action Required</h2>"
                + "<p>A new company has self-registered on Applyra and is pending your approval.</p>"
                + "<table style='border-collapse:collapse;width:100%'>"
                + row("Company", companyName)
                + row("Code", code)
                + row("DB ID", String.valueOf(id))
                + row("Industry", industry != null ? industry : "—")
                + row("Website", website != null ? website : "—")
                + row("HR Name", hrName)
                + row("HR Email", hrEmail)
                + "</table>"
                + "<p style='margin-top:24px'><a href='https://applyra.netlify.app/admin/companies/" + id
                + "' style='background:#7c3aed;color:#fff;padding:10px 20px;border-radius:6px;text-decoration:none'>Review &amp; Approve</a></p>"
                + "</body></html>";
    }

    private static String row(String label, String value) {
        return "<tr><td style='padding:6px 12px;border:1px solid #e5e7eb;background:#f9fafb;font-weight:600'>"
                + label + "</td><td style='padding:6px 12px;border:1px solid #e5e7eb'>" + value + "</td></tr>";
    }
}

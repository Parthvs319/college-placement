package superadmin.company;

import helpers.annotations.SuperAdminRole;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.PasswordUtils;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.superadmin.SuperAdminAccessMiddleware;
import models.body.SuperAdminLoginRequest;
import models.enums.UserType;
import models.repos.CompanyRepository;
import models.repos.UserRepository;
import models.services.EmailService;
import models.sql.Company;
import models.sql.User;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Superadmin approves a self-registered company.
 * Sets company.active=true, user.active=true, user.verified=true,
 * generates a temp password, sends credentials email to the HR.
 *
 * POST /admin/companies/:companyId/approve
 */
@SuperAdminRole
public enum ApproveCompanyController implements BaseController {

    INSTANCE;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";

    @Override
    public void handle(RoutingContext event) {
        SuperAdminAccessMiddleware.INSTANCE.with(event, new ArrayList<>(), this.getClass())
                .map(this::map)
                .subscribe(
                        o -> ResponseUtils.INSTANCE.writeJsonResponse(event, o),
                        error -> ResponseUtils.INSTANCE.handleError(event, error)
                );
    }

    private Object map(SuperAdminLoginRequest request) {
        String companyIdStr = request.getRoutingContext().pathParam("companyId");
        if (companyIdStr == null) throw new RoutingError("companyId path param required");

        Long companyId = Long.parseLong(companyIdStr);
        Company company = CompanyRepository.INSTANCE.byId(companyId);
        if (company == null) throw new RoutingError(404, "Company not found");

        if (company.active) throw new RoutingError("Company is already active");

        // Activate company
        company.active = true;
        company.update();

        // Find the primary HR user for this company and activate
        List<User> hrUsers = UserRepository.INSTANCE.where()
                .eq("company.id", companyId)
                .eq("userType", UserType.COMPANY_HR)
                .eq("isPrimary", true)
                .findList();

        if (hrUsers.isEmpty()) throw new RoutingError("No primary HR user found for this company");

        User hrUser = hrUsers.get(0);
        String rawPassword = generatePassword(12);
        hrUser.password  = PasswordUtils.INSTANCE.hash(rawPassword);
        hrUser.active    = true;
        hrUser.verified  = true;
        hrUser.update();

        // Send credentials email
        final String finalPassword = rawPassword;
        final String hrEmail       = hrUser.email;
        final String hrName        = hrUser.name;
        final String companyName   = company.name;

        EmailService.sendEmail(
                hrEmail,
                "Your Applyra Company Account is Approved — " + companyName,
                buildApprovalHtml(companyName, hrName, hrEmail, finalPassword)
        ).subscribe(
                ok  -> System.out.println("[ApproveCompany] Credentials sent to " + hrEmail),
                err -> System.err.println("[ApproveCompany] Email error: " + err.getMessage())
        );

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("message", "Company approved. Credentials sent to " + hrEmail);
        res.put("companyId", companyId);
        res.put("companyName", companyName);
        res.put("hrEmail", hrEmail);
        return res;
    }

    private String generatePassword(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        return sb.toString();
    }

    private static String buildApprovalHtml(String companyName, String hrName, String email, String password) {
        return "<!DOCTYPE html><html><body style='font-family:sans-serif;color:#1a1a1a;max-width:600px;margin:0 auto;padding:20px'>"
                + "<h2 style='color:#7c3aed'>Welcome to Applyra — " + companyName + "</h2>"
                + "<p>Hi " + hrName + ",</p>"
                + "<p>Your company registration on <strong>Applyra</strong> has been approved. "
                + "You can now log in to manage placement drives and connect with colleges.</p>"
                + "<div style='background:#f9fafb;border:1px solid #e5e7eb;border-radius:8px;padding:16px;margin:20px 0'>"
                + "<p style='margin:0 0 8px'><strong>Login Email:</strong> " + email + "</p>"
                + "<p style='margin:0'><strong>Temporary Password:</strong> <code style='background:#ede9fe;padding:2px 6px;border-radius:4px'>" + password + "</code></p>"
                + "</div>"
                + "<p>Please change your password after your first login.</p>"
                + "<p><a href='https://applyra.netlify.app/login' style='background:#7c3aed;color:#fff;padding:10px 20px;border-radius:6px;text-decoration:none'>Log In to Applyra</a></p>"
                + "<hr style='border:none;border-top:1px solid #e5e7eb;margin:24px 0'>"
                + "<p style='font-size:12px;color:#6b7280'>Questions? Contact us at support@applyra.in</p>"
                + "</body></html>";
    }
}

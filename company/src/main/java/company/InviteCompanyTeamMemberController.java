package company;

import helpers.annotations.CompanyRole;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.PasswordUtils;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.company.CompanyAccessMiddleware;
import models.body.CompanyLoginRequest;
import models.enums.UserType;
import models.repos.InviteTokenRepository;
import models.repos.PortalPermissionRepository;
import models.repos.UserRepository;
import models.services.EmailService;
import models.sql.Company;
import models.sql.InviteToken;
import models.sql.PortalPermission;
import models.sql.User;

import java.sql.Timestamp;
import java.util.*;

/**
 * POST /company/team/invite
 * Primary HR invites a sub-HR with module-level permissions.
 * Body: { name, email, permissions: { drives?, applicants? } }
 * Access levels: "none" | "read" | "write"
 */
@CompanyRole
public enum InviteCompanyTeamMemberController implements BaseController {

    INSTANCE;

    private static final Set<String> VALID_MODULES = Set.of("drives", "applicants");
    private static final Set<String> VALID_LEVELS  = Set.of("none", "read", "write");
    private static final long TOKEN_TTL_MS = 48L * 60 * 60 * 1000; // 48 hours

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
        if (!request.getUser().isPrimary) throw new RoutingError(403, "Only primary HR can invite team members");
        Company company = request.getCompany();
        if (company == null) throw new RoutingError(404, "No company linked to your account");

        var body = request.getRequest();
        String name  = body.get("name");
        String email = body.get("email");

        if (name == null || name.isBlank())   throw new RoutingError("name is required");
        if (email == null || email.isBlank()) throw new RoutingError("email is required");

        final String hrEmail = email.trim().toLowerCase();
        final String hrName  = name.trim();

        // Validate permissions map
        @SuppressWarnings("unchecked")
        Map<String, String> permMap = (Map<String, String>) body.get("permissions");
        if (permMap == null || permMap.isEmpty()) throw new RoutingError("permissions map is required");
        for (Map.Entry<String, String> e : permMap.entrySet()) {
            if (!VALID_MODULES.contains(e.getKey()))
                throw new RoutingError("Unknown module: " + e.getKey());
            if (!VALID_LEVELS.contains(e.getValue()))
                throw new RoutingError("Invalid access level for " + e.getKey() + ": " + e.getValue());
        }

        Long companyId = company.getId();

        // Find or create user
        User user = UserRepository.INSTANCE.byEmail(hrEmail);
        boolean userCreated = false;

        if (user == null) {
            user = new User();
            user.email    = hrEmail;
            user.name     = hrName;
            user.userType = UserType.COMPANY_HR;
            user.company  = company;
            user.verified = false;
            user.active   = false;
            user.isPrimary = false;
            user.password = PasswordUtils.INSTANCE.hash(UUID.randomUUID().toString()); // placeholder
            user.save();
            userCreated = true;
        } else if (user.userType != UserType.COMPANY_HR || !companyId.equals(user.company != null ? user.company.getId() : null)) {
            throw new RoutingError("User with this email already exists with a different role or company");
        }

        // Upsert portal permission
        PortalPermission perm = PortalPermissionRepository.INSTANCE.byUserAndCompany(user.getId(), companyId);
        if (perm == null) {
            perm = new PortalPermission();
            perm.user      = user;
            perm.company   = company;
            perm.createdBy = request.getUser();
            perm.permissions = permMap;
            perm.save();
        } else {
            perm.permissions = permMap;
            perm.update();
        }

        // Create invite token
        InviteToken existing = InviteTokenRepository.INSTANCE.byEmailAndCompany(hrEmail, companyId);
        if (existing != null) {
            existing.used = false;
            existing.expiresAt = new Timestamp(System.currentTimeMillis() + TOKEN_TTL_MS);
            existing.update();
        } else {
            existing = new InviteToken();
            existing.token     = UUID.randomUUID().toString();
            existing.email     = hrEmail;
            existing.company   = company;
            existing.userType  = UserType.COMPANY_HR;
            existing.invitedBy = request.getUser();
            existing.expiresAt = new Timestamp(System.currentTimeMillis() + TOKEN_TTL_MS);
            existing.save();
        }

        final String token       = existing.token;
        final String companyName = company.name;
        final Timestamp expiresAt = existing.expiresAt;

        EmailService.sendEmail(
                hrEmail,
                "You're invited to join " + companyName + " on Applyra",
                buildInviteHtml(companyName, hrName, token, expiresAt.toString())
        ).subscribe(
                ok  -> System.out.println("[CompanyTeamInvite] Invite sent to " + hrEmail),
                err -> System.err.println("[CompanyTeamInvite] Email error: " + err.getMessage())
        );

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("message",   "Invite sent to " + hrEmail);
        res.put("email",     hrEmail);
        res.put("name",      hrName);
        res.put("expiresAt", expiresAt.toString());
        return res;
    }

    private static String buildInviteHtml(String companyName, String name, String token, String expiresAt) {
        String url = "https://applyra.netlify.app/company/accept-invite?token=" + token;
        return "<!DOCTYPE html><html><body style='font-family:sans-serif;color:#1a1a1a;max-width:600px;margin:0 auto;padding:20px'>"
                + "<h2 style='color:#7c3aed'>You've been invited to " + companyName + "</h2>"
                + "<p>Hi " + name + ",</p>"
                + "<p>You've been invited to join the HR team for <strong>" + companyName + "</strong> on Applyra.</p>"
                + "<p>Click the link below to set your password and activate your account:</p>"
                + "<p><a href='" + url + "' style='background:#7c3aed;color:#fff;padding:10px 20px;border-radius:6px;text-decoration:none'>Accept Invite</a></p>"
                + "<p style='font-size:12px;color:#6b7280'>Link expires: " + expiresAt + "</p>"
                + "</body></html>";
    }
}

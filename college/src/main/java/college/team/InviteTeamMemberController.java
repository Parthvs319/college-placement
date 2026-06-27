package college.team;

import helpers.annotations.CollegeRole;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.college.CollegeAccessMiddleware;
import models.body.CollegeLoginRequest;
import models.enums.UserType;
import models.repos.InviteTokenRepository;
import models.repos.PortalPermissionRepository;
import models.repos.UserRepository;
import models.services.EmailService;
import models.sql.InviteToken;
import models.sql.PortalPermission;
import models.sql.User;

import java.sql.Timestamp;
import java.util.*;

@CollegeRole
public enum InviteTeamMemberController implements BaseController {
    INSTANCE;

    private static final long INVITE_EXPIRY_MS = 48L * 60 * 60 * 1000; // 48 hours
    private static final String FRONTEND_URL = System.getenv().getOrDefault("FRONTEND_URL", "https://applyra.netlify.app");

    @Override
    public void handle(RoutingContext event) {
        CollegeAccessMiddleware.INSTANCE.with(event, new ArrayList<>(), this.getClass())
                .map(this::map)
                .subscribe(
                        o -> ResponseUtils.INSTANCE.writeJsonResponse(event, o),
                        error -> ResponseUtils.INSTANCE.handleError(event, error)
                );
    }

    @SuppressWarnings("unchecked")
    private Object map(CollegeLoginRequest req) {
        if (!req.getUser().isPrimary) {
            throw new RoutingError(403, "Only the primary account holder can invite team members");
        }

        String name = (String) req.getRequest().get("name");
        String email = (String) req.getRequest().get("email");
        Object permsObj = req.getRequest().get("permissions");

        if (name == null || name.isBlank()) throw new RoutingError("name is required");
        if (email == null || email.isBlank()) throw new RoutingError("email is required");
        if (permsObj == null) throw new RoutingError("permissions is required");

        email = email.trim().toLowerCase();
        Map<String, String> permissions = (Map<String, String>) permsObj;

        // Validate permission values
        for (Map.Entry<String, String> entry : permissions.entrySet()) {
            String val = entry.getValue();
            if (!"none".equals(val) && !"read".equals(val) && !"write".equals(val)) {
                throw new RoutingError("Invalid permission value '" + val + "' for module '" + entry.getKey() + "'. Use none, read, or write.");
            }
        }

        // Check if user already exists
        User existingUser = UserRepository.INSTANCE.byEmail(email);
        if (existingUser != null) {
            // If already a TPO for this college, just update permissions
            if (existingUser.userType == UserType.TPO
                    && existingUser.college != null
                    && existingUser.college.getId().equals(req.getCollege().getId())) {
                PortalPermission existingPerm = PortalPermissionRepository.INSTANCE
                        .byUserAndCollege(existingUser.getId(), req.getCollege().getId());
                if (existingPerm != null) {
                    existingPerm.permissions = permissions;
                    existingPerm.save();
                    Map<String, Object> r = new LinkedHashMap<>();
                    r.put("message", "Permissions updated for existing team member");
                    r.put("email", email);
                    return r;
                }
            }
            throw new RoutingError(409, "A user with this email already exists on the platform");
        }

        // Create sub-TPO user (not primary, not yet verified)
        User newUser = new User();
        newUser.email = email;
        newUser.name = name.trim();
        newUser.userType = UserType.TPO;
        newUser.college = req.getCollege();
        newUser.isPrimary = false;
        newUser.verified = false;
        newUser.active = true;
        newUser.save();

        // Create portal permission record
        PortalPermission perm = new PortalPermission();
        perm.user = newUser;
        perm.college = req.getCollege();
        perm.permissions = permissions;
        perm.createdBy = req.getUser();
        perm.save();

        // Create invite token (48h expiry)
        InviteToken token = new InviteToken();
        token.token = UUID.randomUUID().toString();
        token.email = email;
        token.college = req.getCollege();
        token.userType = UserType.TPO;
        token.expiresAt = new Timestamp(System.currentTimeMillis() + INVITE_EXPIRY_MS);
        token.invitedBy = req.getUser();
        token.save();

        // Send invite email
        String inviteUrl = FRONTEND_URL + "/accept-invite?token=" + token.token;
        String collegeName = req.getCollege().getName();
        String inviterName = req.getUser().name != null ? req.getUser().name : "Your placement team";
        String html = buildInviteHtml(name.trim(), inviterName, collegeName, inviteUrl);
        EmailService.sendEmail(email, inviterName + " invited you to join " + collegeName + " on Applyra", html)
                .subscribe(ok -> {}, err -> System.err.println("[TeamInvite] Email error: " + err.getMessage()));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "Invitation sent successfully");
        response.put("email", email);
        response.put("name", name.trim());
        response.put("expiresAt", token.expiresAt.toString());
        return response;
    }

    private String buildInviteHtml(String recipientName, String inviterName, String collegeName, String inviteUrl) {
        return "<!DOCTYPE html><html><body style='font-family:sans-serif;max-width:560px;margin:40px auto;color:#111'>"
                + "<h2 style='margin-bottom:4px'>You're invited to join " + collegeName + "</h2>"
                + "<p style='color:#666'>" + inviterName + " has added you as a team member on Applyra.</p>"
                + "<p>Hi " + recipientName + ",</p>"
                + "<p>Click the button below to set your password and access the placement portal. "
                + "This link expires in <strong>48 hours</strong>.</p>"
                + "<a href='" + inviteUrl + "' style='display:inline-block;margin:20px 0;padding:12px 28px;"
                + "background:#1A73E8;color:#fff;border-radius:8px;text-decoration:none;font-weight:600'>Accept Invitation</a>"
                + "<p style='color:#999;font-size:12px'>If you didn't expect this, you can safely ignore this email.</p>"
                + "</body></html>";
    }
}

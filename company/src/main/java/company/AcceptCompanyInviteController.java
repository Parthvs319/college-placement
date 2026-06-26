package company;

import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.PasswordUtils;
import helpers.utils.ResponseUtils;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.repos.InviteTokenRepository;
import models.repos.UserRepository;
import models.sql.InviteToken;
import models.sql.User;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * POST /company/accept-invite
 * Public endpoint — HR user sets their password using an invite token.
 * Body: { token, password }
 */
public enum AcceptCompanyInviteController implements BaseController {

    INSTANCE;

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

        String token    = body.getString("token");
        String password = body.getString("password");

        if (token == null || token.isBlank())       throw new RoutingError("token is required");
        if (password == null || password.length() < 6) throw new RoutingError("password must be at least 6 characters");

        InviteToken invite = InviteTokenRepository.INSTANCE.findValidToken(token);
        if (invite == null) throw new RoutingError(400, "Invite link is invalid or has expired");
        if (invite.company == null) throw new RoutingError(400, "This invite is not for a company HR account");

        User user = UserRepository.INSTANCE.byEmail(invite.email);
        if (user == null) throw new RoutingError(404, "User account not found");

        user.password = PasswordUtils.INSTANCE.hash(password);
        user.active   = true;
        user.verified = true;
        user.update();

        invite.used = true;
        invite.update();

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("message",     "Account activated! You can now log in.");
        res.put("email",       user.email);
        res.put("companyName", invite.company.name);
        return res;
    }
}

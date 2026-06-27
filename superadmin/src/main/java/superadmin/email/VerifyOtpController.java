package superadmin.email;

import helpers.annotations.SuperAdminRole;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.superadmin.SuperAdminAccessMiddleware;
import models.body.SuperAdminLoginRequest;
import models.services.OtpService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * POST /admin/verify-otp
 *
 * Verifies the OTP submitted by the user for an email or phone number.
 *
 * Request body: { "type": "email" | "phone", "value": "...", "otp": "123456" }
 * Response:     { "success": true|false, "message": "..." }
 */
@SuperAdminRole
public enum VerifyOtpController implements BaseController {

    INSTANCE;

    @Override
    public void handle(RoutingContext event) {
        SuperAdminAccessMiddleware.INSTANCE.with(event, new ArrayList<>(), this.getClass())
                .map(req -> verify(req, event))
                .subscribe(
                        o -> ResponseUtils.INSTANCE.writeJsonResponse(event, o),
                        error -> ResponseUtils.INSTANCE.handleError(event, error)
                );
    }

    private Object verify(SuperAdminLoginRequest req, RoutingContext rc) {
        io.vertx.core.json.JsonObject body = rc.body().asJsonObject();
        if (body == null) throw new RoutingError("Request body is required");

        String type  = body.getString("type",  "").trim().toLowerCase();
        String value = body.getString("value", "").trim();
        String otp   = body.getString("otp",   "").trim();

        if (!type.equals("email") && !type.equals("phone"))
            throw new RoutingError("type must be 'email' or 'phone'");
        if (value.isEmpty()) throw new RoutingError("value is required");
        if (otp.isEmpty())   throw new RoutingError("otp is required");
        if (!otp.matches("\\d{6}")) throw new RoutingError("OTP must be 6 digits");

        OtpService.VerifyResult result = OtpService.INSTANCE.verify(type, value, otp);

        Map<String, Object> out = new HashMap<>();
        out.put("success", result.isSuccess());
        out.put("message", result.message());
        out.put("verified", result.isSuccess());
        return out;
    }
}

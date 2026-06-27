package superadmin.user;

import helpers.annotations.SuperAdminRole;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.superadmin.SuperAdminAccessMiddleware;
import models.enums.UserType;
import models.repos.UserRepository;
import models.services.OtpService;
import models.sql.User;

import java.util.ArrayList;
import java.util.Map;

@SuperAdminRole(request = {"otp:string"})
public enum ToggleUserActiveController implements BaseController {

    INSTANCE;

    @Override
    public void handle(RoutingContext event) {
        SuperAdminAccessMiddleware.INSTANCE.with(event, new ArrayList<>(), this.getClass())
                .map(req -> {
                    String adminEmail = req.getUser().getEmail();
                    String otp = req.getRequest().get("otp");
                    OtpService.VerifyResult result = OtpService.INSTANCE.verify("admin-action", adminEmail, otp);
                    if (result != OtpService.VerifyResult.SUCCESS)
                        throw new RoutingError(400, "Invalid or expired OTP. Please request a new one.");

                    Long userId = Long.parseLong(event.pathParam("userId"));
                    User user = UserRepository.INSTANCE.byId(userId);
                    if (user == null) throw new RoutingError(404, "User not found");
                    if (user.getUserType() == UserType.SUPER_ADMIN && !user.getId().equals(req.getUser().getId())) {
                        throw new RoutingError(403, "Cannot modify another super admin");
                    }
                    user.setActive(!user.isActive());
                    user.update();

                    return Map.of(
                            "message", user.isActive() ? "User activated" : "User deactivated",
                            "userId", userId,
                            "active", user.isActive()
                    );
                })
                .subscribe(
                        o -> ResponseUtils.INSTANCE.writeJsonResponse(event, o),
                        error -> ResponseUtils.INSTANCE.handleError(event, error)
                );
    }
}

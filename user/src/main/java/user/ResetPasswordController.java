package user;

import helpers.blueprint.enums.RequestItemType;
import helpers.customErrors.RoutingError;
import helpers.interfaces.ParamsController;
import helpers.utils.PasswordUtils;
import helpers.utils.RequestItem;
import helpers.utils.RequestZipped;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.core.RxHelper;
import io.vertx.rxjava.ext.web.RoutingContext;
import lombok.Data;
import models.repos.UserRepository;
import models.services.OtpService;
import models.sql.User;
import rx.Single;

import java.util.ArrayList;
import java.util.List;

/**
 * POST /user/reset-password
 * Body: { email, otp, newPassword }
 *
 * Verifies the OTP issued by ForgotPasswordController and sets the new password.
 */
public enum ResetPasswordController implements ParamsController {

    INSTANCE;

    @Override
    public void handle(RoutingContext event) {
        Single.just(event)
                .subscribeOn(RxHelper.blockingScheduler(event.vertx()))
                .map(this::map)
                .map(this::map)
                .subscribe(
                        o -> ResponseUtils.INSTANCE.writeJsonResponse(event, o),
                        error -> ResponseUtils.INSTANCE.handleError(event, error)
                );
    }

    private Response map(RequestZipped ctx) {
        String email       = ((String) ctx.getRequest().get("email")).trim().toLowerCase();
        String otp         = ((String) ctx.getRequest().get("otp")).trim();
        String newPassword = (String) ctx.getRequest().get("newPassword");

        if (newPassword == null || newPassword.length() < 6) {
            throw new RoutingError("Password must be at least 6 characters.");
        }

        // Verify OTP
        OtpService.VerifyResult result = OtpService.INSTANCE.verify("password-reset", email, otp);
        if (!result.isSuccess()) {
            throw new RoutingError(result.message());
        }

        // Find user and update password
        User user = UserRepository.INSTANCE.byEmail(email);
        if (user == null || !user.isActive()) {
            throw new RoutingError("Account not found or inactive.");
        }

        user.setPassword(PasswordUtils.INSTANCE.hash(newPassword));
        user.update();

        return new Response("Password reset successfully. You can now log in.");
    }

    @Override
    public List<RequestItem> items() {
        List<RequestItem> items = new ArrayList<>();
        items.add(RequestItem.builder().key("email").required(true).itemType(RequestItemType.STRING).build());
        items.add(RequestItem.builder().key("otp").required(true).itemType(RequestItemType.STRING).build());
        items.add(RequestItem.builder().key("newPassword").required(true).itemType(RequestItemType.STRING).build());
        return items;
    }

    @Data
    static class Response {
        String message;
        Response(String message) { this.message = message; }
    }
}

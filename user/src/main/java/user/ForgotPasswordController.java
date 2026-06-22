package user;

import helpers.blueprint.enums.RequestItemType;
import helpers.customErrors.RoutingError;
import helpers.interfaces.ParamsController;
import helpers.utils.RequestItem;
import helpers.utils.RequestZipped;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.core.RxHelper;
import io.vertx.rxjava.ext.web.RoutingContext;
import lombok.Data;
import models.repos.UserRepository;
import models.services.EmailService;
import models.services.OtpService;
import models.sql.User;
import rx.Single;

import java.util.ArrayList;
import java.util.List;

/**
 * POST /user/forgot-password
 * Body: { email }
 *
 * Generates a 6-digit OTP, stores it in OtpService, and emails it to the user.
 * Always returns success (to avoid email enumeration).
 */
public enum ForgotPasswordController implements ParamsController {

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
        String email = ((String) ctx.getRequest().get("email")).trim().toLowerCase();

        User user = UserRepository.INSTANCE.byEmail(email);

        // Generate OTP regardless — don't leak whether the email exists
        String otp = OtpService.INSTANCE.generate("password-reset", email);

        if (user != null && user.isActive()) {
            String html = EmailService.buildPasswordResetHtml(user.getName(), otp);
            EmailService.sendEmail(email, "Reset your Applyra password", html)
                    .subscribe(
                            sent -> System.out.println("[ForgotPassword] OTP email " + (sent ? "sent" : "failed") + " to " + email),
                            err  -> System.err.println("[ForgotPassword] Email error: " + err.getMessage())
                    );
        }

        return new Response("If this email is registered, a reset OTP has been sent.");
    }

    @Override
    public List<RequestItem> items() {
        List<RequestItem> items = new ArrayList<>();
        items.add(RequestItem.builder()
                .key("email")
                .required(true)
                .itemType(RequestItemType.STRING)
                .build());
        return items;
    }

    @Data
    static class Response {
        String message;
        Response(String message) { this.message = message; }
    }
}

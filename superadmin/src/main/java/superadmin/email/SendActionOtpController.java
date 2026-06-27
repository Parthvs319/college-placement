package superadmin.email;

import helpers.annotations.SuperAdminRole;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.superadmin.SuperAdminAccessMiddleware;
import models.services.EmailService;
import models.services.OtpService;

import java.util.ArrayList;
import java.util.Map;

/**
 * POST /admin/send-action-otp
 *
 * Generates a 6-digit admin-action OTP and delivers it to the authenticated
 * super admin's email address. Used to confirm destructive/sensitive actions
 * (e.g. toggling college/user active state).
 *
 * No request body required.
 * Response: { "message": "OTP sent to {email}" }
 */
@SuperAdminRole
public enum SendActionOtpController implements BaseController {

    INSTANCE;

    @Override
    public void handle(RoutingContext event) {
        SuperAdminAccessMiddleware.INSTANCE.with(event, new ArrayList<>(), this.getClass())
                .map(req -> {
                    String adminEmail = req.getUser().getEmail();
                    String adminName  = req.getUser().getName();

                    String otp = OtpService.INSTANCE.generate("admin-action", adminEmail);

                    String htmlBody = EmailService.buildCustomHtml(
                            adminName,
                            "Action Confirmation",
                            "Your OTP for this action is: <strong>" + otp + "</strong>. It is valid for 10 minutes."
                    );

                    new Thread(() ->
                            EmailService.sendEmail(adminEmail, "Applyra — Action Confirmation OTP", htmlBody)
                                    .subscribe(
                                            sent -> System.out.println("[SendActionOtp] Email sent=" + sent + " to " + adminEmail),
                                            err  -> System.err.println("[SendActionOtp] Email error: " + err.getMessage())
                                    )
                    ).start();

                    return (Object) Map.of("message", "OTP sent to " + adminEmail);
                })
                .subscribe(
                        o     -> ResponseUtils.INSTANCE.writeJsonResponse(event, o),
                        error -> ResponseUtils.INSTANCE.handleError(event, error)
                );
    }
}

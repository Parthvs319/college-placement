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
import models.services.OtpService;
import rx.Single;

import java.util.ArrayList;
import java.util.List;

/**
 * POST /user/verify-reset-otp
 * Body: { email, otp }
 *
 * Peeks (validates without consuming) the password-reset OTP.
 * If valid, the frontend can proceed to the "set new password" step.
 * The OTP is consumed by /user/reset-password on the final step.
 */
public enum VerifyResetOtpController implements ParamsController {

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
        String otp   = ((String) ctx.getRequest().get("otp")).trim();

        OtpService.VerifyResult result = OtpService.INSTANCE.peek("password-reset", email, otp);
        if (!result.isSuccess()) {
            throw new RoutingError(result.message());
        }

        return new Response("OTP verified.");
    }

    @Override
    public List<RequestItem> items() {
        List<RequestItem> items = new ArrayList<>();
        items.add(RequestItem.builder().key("email").required(true).itemType(RequestItemType.STRING).build());
        items.add(RequestItem.builder().key("otp").required(true).itemType(RequestItemType.STRING).build());
        return items;
    }

    @Data
    static class Response {
        String message;
        Response(String message) { this.message = message; }
    }
}

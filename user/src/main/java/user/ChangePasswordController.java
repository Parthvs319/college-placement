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
import models.sql.User;
import rx.Single;

import java.util.ArrayList;
import java.util.List;

/**
 * POST /user/change-password
 * Body: { email, currentPassword, newPassword }
 *
 * Verifies currentPassword matches the stored hash, then sets newPassword.
 * No auth token required — the current password itself is the credential.
 */
public enum ChangePasswordController implements ParamsController {

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
        String email           = ((String) ctx.getRequest().get("email")).trim().toLowerCase();
        String currentPassword = (String) ctx.getRequest().get("currentPassword");
        String newPassword     = (String) ctx.getRequest().get("newPassword");

        if (newPassword == null || newPassword.length() < 6) {
            throw new RoutingError("New password must be at least 6 characters.");
        }

        User user = UserRepository.INSTANCE.byEmail(email);
        if (user == null || !user.isActive()) {
            throw new RoutingError("Account not found.");
        }

        if (!PasswordUtils.INSTANCE.match(currentPassword, user.getPassword())) {
            throw new RoutingError("Current password is incorrect.");
        }

        if (currentPassword.equals(newPassword)) {
            throw new RoutingError("New password must be different from the current password.");
        }

        user.setPassword(PasswordUtils.INSTANCE.hash(newPassword));
        user.update();

        return new Response("Password changed successfully.");
    }

    @Override
    public List<RequestItem> items() {
        List<RequestItem> items = new ArrayList<>();
        items.add(RequestItem.builder().key("email").required(true).itemType(RequestItemType.STRING).build());
        items.add(RequestItem.builder().key("currentPassword").required(true).itemType(RequestItemType.STRING).build());
        items.add(RequestItem.builder().key("newPassword").required(true).itemType(RequestItemType.STRING).build());
        return items;
    }

    @Data
    static class Response {
        String message;
        Response(String message) { this.message = message; }
    }
}

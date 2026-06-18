package superadmin;

import helpers.annotations.SuperAdminRole;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.superadmin.SuperAdminAccessMiddleware;
import models.enums.CreditTransactionType;
import models.services.CreditService;
import models.sql.Subscription;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * POST /admin/subscriptions/:subscriptionId/top-up
 * Body: { credits: 100, paymentReference: "pay_xxx", description: "..." }
 * Adds credits to a subscription (admin or payment callback).
 */
@SuperAdminRole
public enum CreditTopUpController implements BaseController {

    INSTANCE;

    @Override
    public void handle(RoutingContext event) {
        SuperAdminAccessMiddleware.INSTANCE.with(event, new ArrayList<>(), this.getClass())
                .map(req -> {
                    Long subscriptionId = Long.parseLong(event.pathParam("subscriptionId"));

                    int credits = Integer.parseInt(req.getRequest().get("credits"));
                    String paymentRef = req.getRequest().getOrDefault("paymentReference", null);
                    String description = req.getRequest().getOrDefault("description", "Admin credit top-up");
                    String typeStr = req.getRequest().getOrDefault("type", "ADMIN_TOPUP");

                    CreditTransactionType type;
                    try {
                        type = CreditTransactionType.valueOf(typeStr);
                    } catch (IllegalArgumentException e) {
                        type = CreditTransactionType.ADMIN_TOPUP;
                    }

                    Subscription sub;
                    if (paymentRef != null && !paymentRef.isEmpty()) {
                        sub = CreditService.INSTANCE.topUpCredits(subscriptionId, credits, paymentRef, description);
                    } else {
                        sub = CreditService.INSTANCE.adminTopUp(subscriptionId, credits, type, description);
                    }

                    Map<String, Object> result = new HashMap<>();
                    result.put("success", true);
                    result.put("subscriptionId", sub.getId());
                    result.put("totalCredits", sub.totalCredits);
                    result.put("usedCredits", sub.usedCredits);
                    result.put("remainingCredits", sub.totalCredits - sub.usedCredits);
                    result.put("creditsAdded", credits);
                    return result;
                })
                .subscribe(
                        o -> ResponseUtils.INSTANCE.writeJsonResponse(event, o),
                        error -> ResponseUtils.INSTANCE.handleError(event, error)
                );
    }
}

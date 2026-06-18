package superadmin;

import helpers.annotations.SuperAdminRole;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.superadmin.SuperAdminAccessMiddleware;
import models.repos.CreditTransactionRepository;
import models.sql.CreditTransaction;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * GET /admin/subscriptions/:subscriptionId/credits
 * Returns credit transaction history for a subscription.
 */
@SuperAdminRole
public enum CreditHistoryController implements BaseController {

    INSTANCE;

    @Override
    public void handle(RoutingContext event) {
        SuperAdminAccessMiddleware.INSTANCE.with(event, new ArrayList<>(), this.getClass())
                .map(req -> {
                    Long subscriptionId = Long.parseLong(event.pathParam("subscriptionId"));

                    List<CreditTransaction> txns = CreditTransactionRepository.INSTANCE.bySubscription(subscriptionId);

                    return txns.stream().map(t -> {
                        SuperAdminDtos.CreditTransactionDto dto = new SuperAdminDtos.CreditTransactionDto();
                        dto.setId(t.getId());
                        dto.setType(t.type != null ? t.type.name() : null);
                        dto.setAmount(t.amount);
                        dto.setBalanceAfter(t.balanceAfter);
                        dto.setDescription(t.description);
                        dto.setPaymentReference(t.paymentReference);
                        dto.setCreatedAt(t.getCreatedAt() != null ? t.getCreatedAt().toInstant().toString() : null);

                        if (t.student != null && t.student.user != null) {
                            dto.setStudentName(t.student.user.name);
                        }
                        if (t.college != null) {
                            dto.setCollegeName(t.college.name);
                        }
                        return dto;
                    }).collect(Collectors.toList());
                })
                .subscribe(
                        o -> ResponseUtils.INSTANCE.writeJsonResponse(event, o),
                        error -> ResponseUtils.INSTANCE.handleError(event, error)
                );
    }
}

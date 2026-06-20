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
                        dto.setType(t.getType() != null ? t.getType().name() : null);
                        dto.setAmount(t.getAmount());
                        dto.setBalanceAfter(t.getBalanceAfter());
                        dto.setDescription(t.getDescription());
                        dto.setPaymentReference(t.getPaymentReference());
                        dto.setCreatedAt(t.getCreatedAt() != null ? t.getCreatedAt().toInstant().toString() : null);

                        if (t.getStudent() != null && t.getStudent().getUser() != null) {
                            dto.setStudentName(t.getStudent().getUser().getName());
                        }
                        if (t.getCollege() != null) {
                            dto.setCollegeName(t.getCollege().getName());
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

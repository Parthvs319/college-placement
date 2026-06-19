package superadmin;

import helpers.annotations.SuperAdminRole;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.superadmin.SuperAdminAccessMiddleware;
import models.repos.SubscriptionRepository;
import models.sql.Subscription;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@SuperAdminRole
public enum ListAllSubscriptionsController implements BaseController {

    INSTANCE;

    @Override
    public void handle(RoutingContext event) {
        SuperAdminAccessMiddleware.INSTANCE.with(event, new ArrayList<>(), this.getClass())
                .map(req -> {
                    List<Subscription> subs = SubscriptionRepository.INSTANCE.findAllEager();

                    return subs.stream().map(s -> {
                        SuperAdminDtos.SubscriptionOverview o = new SuperAdminDtos.SubscriptionOverview();
                        o.setId(s.getId());
                        o.setTier(s.getTier() != null ? s.getTier().name() : null);
                        o.setActive(s.isActive());
                        o.setStartDate(s.getStartDate() != null ? s.getStartDate().toString() : null);
                        o.setEndDate(s.getEndDate() != null ? s.getEndDate().toString() : null);

                        if (s.getStudent() != null && s.getStudent().user != null) {
                            o.setStudentName(s.getStudent().getUser().getName());
                        }
                        if (s.getCollege() != null) {
                            o.setCollegeName(s.getCollege().getName());
                        } else if (s.getStudent() != null && s.getStudent().getCollege() != null) {
                            o.setCollegeName(s.getStudent().getCollege().getName());
                        }

                        o.setTotalCredits(s.totalCredits);
                        o.setUsedCredits(s.usedCredits);
                        o.setRemainingCredits(s.totalCredits - s.usedCredits);
                        return o;
                    }).collect(Collectors.toList());
                })
                .subscribe(
                        o -> ResponseUtils.INSTANCE.writeJsonResponse(event, o),
                        error -> ResponseUtils.INSTANCE.handleError(event, error)
                );
    }
}

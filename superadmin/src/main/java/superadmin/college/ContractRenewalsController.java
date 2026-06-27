package superadmin.college;

import superadmin.SuperAdminDtos;

import helpers.annotations.SuperAdminRole;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.superadmin.SuperAdminAccessMiddleware;
import models.repos.CollegeContractRepository;
import models.sql.CollegeContract;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * GET /admin/contract-renewals
 * Returns all active contracts expiring within the next 90 days, sorted by urgency (soonest first).
 */
@SuperAdminRole
public enum ContractRenewalsController implements BaseController {

    INSTANCE;

    @Override
    public void handle(RoutingContext event) {
        SuperAdminAccessMiddleware.INSTANCE.with(event, new ArrayList<>(), this.getClass())
                .map(req -> {
                    LocalDate today = LocalDate.now();
                    LocalDate in90Days = today.plusDays(90);

                    List<CollegeContract> contracts = CollegeContractRepository.INSTANCE
                            .expiringBetween(today.toString(), in90Days.toString());

                    return contracts.stream().map(contract -> {
                        SuperAdminDtos.ContractRenewalItem item = new SuperAdminDtos.ContractRenewalItem();
                        item.setContractId(contract.getId());
                        item.setContractType(contract.getContractType());
                        item.setValidFrom(contract.getValidFrom());
                        item.setValidTo(contract.getValidTo());

                        long days = ChronoUnit.DAYS.between(today, LocalDate.parse(contract.getValidTo()));
                        item.setDaysRemaining((int) days);

                        if (contract.getCollege() != null) {
                            item.setCollegeId(contract.getCollege().getId());
                            item.setCollegeName(contract.getCollege().getName());
                            item.setCollegeCode(contract.getCollege().getCode());
                            item.setContactEmail(contract.getCollege().getContactEmail());
                            item.setTpoName(contract.getCollege().getTpoName());
                        }

                        return item;
                    }).collect(Collectors.toList());
                })
                .subscribe(
                        o -> ResponseUtils.INSTANCE.writeJsonResponse(event, o),
                        error -> ResponseUtils.INSTANCE.handleError(event, error)
                );
    }
}

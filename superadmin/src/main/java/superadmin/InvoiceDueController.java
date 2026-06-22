package superadmin;

import helpers.annotations.SuperAdminRole;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.superadmin.SuperAdminAccessMiddleware;
import models.repos.CollegeContractRepository;
import models.sql.CollegeContract;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * GET /admin/invoice-due
 *
 * Returns PAID active contracts whose next invoice cycle is approaching:
 *   - MONTHLY  → within the next 7 days
 *   - YEARLY   → within the next 30 days
 *
 * Next invoice date is calculated from validFrom + the smallest multiple of the
 * billing period that puts the date in the future.
 */
@SuperAdminRole
public enum InvoiceDueController implements BaseController {

    INSTANCE;

    private static final int MONTHLY_WINDOW_DAYS = 7;
    private static final int YEARLY_WINDOW_DAYS  = 30;

    @Override
    public void handle(RoutingContext event) {
        SuperAdminAccessMiddleware.INSTANCE.with(event, new ArrayList<>(), this.getClass())
                .map(req -> buildList())
                .subscribe(
                        o -> ResponseUtils.INSTANCE.writeJsonResponse(event, o),
                        error -> ResponseUtils.INSTANCE.handleError(event, error)
                );
    }

    private List<SuperAdminDtos.InvoiceDueItem> buildList() {
        LocalDate today = LocalDate.now();
        List<CollegeContract> contracts = CollegeContractRepository.INSTANCE.findActiveForInvoiceDue();

        return contracts.stream()
                .map(c -> toItem(c, today))
                .filter(item -> item != null)
                .sorted((a, b) -> Integer.compare(a.getDaysUntilDue(), b.getDaysUntilDue()))
                .collect(Collectors.toList());
    }

    private SuperAdminDtos.InvoiceDueItem toItem(CollegeContract contract, LocalDate today) {
        if (contract.getValidFrom() == null) return null;

        LocalDate validFrom;
        try {
            validFrom = LocalDate.parse(contract.getValidFrom());
        } catch (Exception e) {
            return null;
        }

        String payType = contract.getPayType() != null ? contract.getPayType().toUpperCase() : "MONTHLY";
        boolean isYearly = "YEARLY".equals(payType);

        // Calculate next invoice date: smallest future date that falls on a cycle boundary
        LocalDate nextInvoice = computeNextInvoiceDate(validFrom, today, isYearly);
        if (nextInvoice == null) return null;

        long daysUntilDue = ChronoUnit.DAYS.between(today, nextInvoice);
        int window = isYearly ? YEARLY_WINDOW_DAYS : MONTHLY_WINDOW_DAYS;

        // Only include if within the alert window (and not already overdue by more than 7 days)
        if (daysUntilDue > window || daysUntilDue < -7) return null;

        // Billing period: previous cycle date → day before nextInvoice
        LocalDate billingStart = isYearly
                ? nextInvoice.minusYears(1)
                : nextInvoice.minusMonths(1);
        LocalDate billingEnd = nextInvoice.minusDays(1);

        SuperAdminDtos.InvoiceDueItem item = new SuperAdminDtos.InvoiceDueItem();
        item.setContractId(contract.getId());
        item.setPayType(payType);
        item.setValidFrom(contract.getValidFrom());
        item.setValidTo(contract.getValidTo());
        item.setNextInvoiceDate(nextInvoice.toString());
        item.setDaysUntilDue((int) daysUntilDue);
        item.setBillingPeriodStart(billingStart.toString());
        item.setBillingPeriodEnd(billingEnd.toString());

        // Amount display
        if (contract.getContractAmount() != null) {
            NumberFormat nf = NumberFormat.getNumberInstance(new Locale("en", "IN"));
            BigDecimal display = isYearly
                    ? contract.getContractAmount()
                    : contract.getContractAmount().divide(BigDecimal.valueOf(12), 0, java.math.RoundingMode.HALF_UP);
            item.setContractAmountDisplay("₹" + nf.format(display));
        } else {
            item.setContractAmountDisplay("—");
        }

        if (contract.getCollege() != null) {
            item.setCollegeId(contract.getCollege().getId());
            item.setCollegeName(contract.getCollege().getName());
            item.setCollegeCode(contract.getCollege().getCode());
            item.setContactEmail(contract.getCollege().getContactEmail());
        }

        return item;
    }

    /**
     * Given a validFrom anchor and today, compute the next invoice date.
     * For MONTHLY: anchor.plusMonths(n) where n is the smallest positive integer
     *              such that the result is >= today.
     * For YEARLY:  same but with years.
     */
    private LocalDate computeNextInvoiceDate(LocalDate anchor, LocalDate today, boolean yearly) {
        if (anchor.isAfter(today)) {
            // Contract hasn't started yet — first invoice is on the start date
            return anchor;
        }
        if (yearly) {
            long yearsElapsed = ChronoUnit.YEARS.between(anchor, today);
            LocalDate candidate = anchor.plusYears(yearsElapsed);
            if (!candidate.isAfter(today.minusDays(1))) candidate = candidate.plusYears(1);
            return candidate;
        } else {
            long monthsElapsed = ChronoUnit.MONTHS.between(anchor, today);
            LocalDate candidate = anchor.plusMonths(monthsElapsed);
            if (!candidate.isAfter(today.minusDays(1))) candidate = candidate.plusMonths(1);
            return candidate;
        }
    }
}

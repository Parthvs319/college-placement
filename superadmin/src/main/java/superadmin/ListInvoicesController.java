package superadmin;

import helpers.annotations.SuperAdminRole;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.superadmin.SuperAdminAccessMiddleware;
import models.body.SuperAdminLoginRequest;
import models.repos.CollegeInvoiceRepository;
import models.repos.CollegeRepository;
import models.services.S3Service;
import models.sql.College;
import models.sql.CollegeInvoice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GET /admin/colleges/:collegeId/invoices
 *
 * Returns all invoices for a college in descending order (newest first).
 * Each invoice includes a fresh pre-signed download URL.
 */
@SuperAdminRole
public enum ListInvoicesController implements BaseController {

    INSTANCE;

    @Override
    public void handle(RoutingContext event) {
        SuperAdminAccessMiddleware.INSTANCE.with(event, new ArrayList<>(), this.getClass())
                .map(req -> map(req, event))
                .subscribe(
                        o -> ResponseUtils.INSTANCE.writeJsonResponse(event, o),
                        error -> ResponseUtils.INSTANCE.handleError(event, error)
                );
    }

    private Object map(SuperAdminLoginRequest request, RoutingContext rc) {
        Long collegeId = Long.parseLong(rc.pathParam("collegeId"));
        College college = CollegeRepository.INSTANCE.byId(collegeId);
        if (college == null) throw new RoutingError(404, "College not found");

        List<CollegeInvoice> invoices = CollegeInvoiceRepository.INSTANCE.byCollege(collegeId);

        List<Map<String, Object>> result = invoices.stream().map(inv -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id",                  inv.getId());
            m.put("invoiceNumber",       inv.getInvoiceNumber());
            m.put("amount",              inv.getAmount());
            m.put("contractAmount",      inv.getContractAmount());
            m.put("billingPeriodStart",  inv.getBillingPeriodStart());
            m.put("billingPeriodEnd",    inv.getBillingPeriodEnd());
            m.put("description",         inv.getDescription());
            m.put("status",              inv.getStatus());
            m.put("createdAt",           inv.getCreatedAt() != null ? inv.getCreatedAt().toString() : null);

            // Contract link
            if (inv.getContract() != null) {
                m.put("contractId",      inv.getContract().getId());
                m.put("contractNumber",  inv.getContract().getContractNumber());
                m.put("contractType",    inv.getContract().getContractType());
                m.put("payType",         inv.getContract().getPayType());
            }

            // Fresh download URL
            String url = inv.getFileUrl();
            if (url != null && !url.startsWith("http") && inv.getS3Key() != null) {
                url = S3Service.getDownloadUrl(inv.getS3Key());
            }
            m.put("fileUrl", url);

            if (inv.getGeneratedBy() != null) {
                m.put("generatedByName", inv.getGeneratedBy().getName());
            }

            // Payment details
            m.put("paymentMode",      inv.getPaymentMode());
            m.put("paymentReference", inv.getPaymentReference());
            m.put("paidAt",           inv.getPaidAt());
            m.put("paidByName",       inv.getPaidByName());
            m.put("paymentNotes",     inv.getPaymentNotes());

            return m;
        }).toList();

        return result;
    }
}

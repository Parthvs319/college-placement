package superadmin;

import helpers.annotations.SuperAdminRole;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.superadmin.SuperAdminAccessMiddleware;
import models.body.SuperAdminLoginRequest;
import models.repos.CollegeInvoiceRepository;
import models.sql.CollegeInvoice;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * POST /admin/colleges/:collegeId/invoices/:invoiceId/mark-paid
 *
 * Body (JSON):
 *   {
 *     "paymentMode":      "UPI",          // UPI | NEFT | RTGS | IMPS | CASH | CHEQUE  (required)
 *     "paymentReference": "UTR123456789", // transaction ID / UTR / cheque no (optional)
 *     "paidAt":           "2025-06-22",   // ISO date or datetime (defaults to now)
 *     "paidByName":       "Ramesh Kumar", // payer name (optional)
 *     "paymentNotes":     "Received via HDFC" (optional)
 *   }
 *
 * Marks the invoice as PAID and records payment details.
 */
@SuperAdminRole(request = {
        "paymentMode:string@required",
        "paymentReference:string",
        "paidAt:string",
        "paidByName:string",
        "paymentNotes:string"
})
public enum MarkInvoicePaidController implements BaseController {

    INSTANCE;

    private static final java.util.Set<String> VALID_MODES =
            java.util.Set.of("UPI", "NEFT", "RTGS", "IMPS", "CASH", "CHEQUE");

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
        Long invoiceId = Long.parseLong(rc.pathParam("invoiceId"));
        CollegeInvoice invoice = CollegeInvoiceRepository.INSTANCE.byId(invoiceId);
        if (invoice == null) throw new RoutingError(404, "Invoice not found");
        if ("PAID".equals(invoice.getStatus())) throw new RoutingError("Invoice is already marked as paid.");

        var body = request.getRequest();
        String mode = ((String) body.get("paymentMode")).trim().toUpperCase();
        if (!VALID_MODES.contains(mode)) {
            throw new RoutingError("Invalid paymentMode. Must be one of: UPI, NEFT, RTGS, IMPS, CASH, CHEQUE");
        }

        String reference = body.get("paymentReference");
        String paidAt    = body.get("paidAt");
        String paidBy    = body.get("paidByName");
        String notes     = body.get("paymentNotes");

        // Default paidAt to now
        if (paidAt == null || paidAt.isBlank()) {
            paidAt = LocalDateTime.now().toString().substring(0, 19); // "2025-06-22T14:30:00"
        }

        invoice.setStatus("PAID");
        invoice.setPaymentMode(mode);
        invoice.setPaymentReference(reference != null && !reference.isBlank() ? reference.trim() : null);
        invoice.setPaidAt(paidAt.trim());
        invoice.setPaidByName(paidBy != null && !paidBy.isBlank() ? paidBy.trim() : null);
        invoice.setPaymentNotes(notes != null && !notes.isBlank() ? notes.trim() : null);
        invoice.update();

        Map<String, Object> result = new HashMap<>();
        result.put("invoiceId",        invoice.getId());
        result.put("invoiceNumber",    invoice.getInvoiceNumber());
        result.put("status",           "PAID");
        result.put("paymentMode",      mode);
        result.put("paymentReference", invoice.getPaymentReference());
        result.put("paidAt",           invoice.getPaidAt());
        result.put("paidByName",       invoice.getPaidByName());
        result.put("paymentNotes",     invoice.getPaymentNotes());
        result.put("message",          "Invoice " + invoice.getInvoiceNumber() + " marked as paid.");
        return result;
    }
}

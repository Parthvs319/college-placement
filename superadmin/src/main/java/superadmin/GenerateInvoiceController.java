package superadmin;

import helpers.annotations.SuperAdminRole;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.ebean.DB;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.superadmin.SuperAdminAccessMiddleware;
import models.body.SuperAdminLoginRequest;
import models.repos.CollegeContractRepository;
import models.repos.CollegeInvoiceRepository;
import models.repos.CollegeRepository;
import models.repos.UserRepository;
import models.services.EmailService;
import models.services.InvoiceService;
import models.services.S3Service;
import models.enums.UserType;
import models.sql.*;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * POST /admin/colleges/:collegeId/invoice
 *
 * Body (JSON):
 *   {
 *     "billingPeriodStart": "2025-01-01",
 *     "billingPeriodEnd":   "2025-03-31",
 *     "description":        "Q1 2025 Platform Fee"   (optional)
 *   }
 *
 * Workflow:
 *   1. Fetch latest active contract → get contractAmount
 *   2. Generate invoice number: INV-{CODE}-{YEAR}-{SEQ}
 *   3. Generate PDF with InvoiceService
 *   4. Upload PDF to S3 → invoices/{uuid}.pdf
 *   5. Save CollegeInvoice to DB
 *   6. Fire-and-forget: email invoice to college contact email
 *   7. Return invoice details + download URL
 */
@SuperAdminRole(request = {
        "billingPeriodStart:string",
        "billingPeriodEnd:string",
        "description:string"
})
public enum GenerateInvoiceController implements BaseController {

    INSTANCE;

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DISPLAY = DateTimeFormatter.ofPattern("dd MMM yyyy");

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

        var body = request.getRequest();
        String billingStart = body.get("billingPeriodStart");
        String billingEnd   = body.get("billingPeriodEnd");
        String description  = body.get("description");

        // ── Fetch latest contract ─────────────────────────────────
        CollegeContract contract = CollegeContractRepository.INSTANCE.latestActive(collegeId);
        if (contract == null) contract = CollegeContractRepository.INSTANCE.latest(collegeId);
        if (contract == null) throw new RoutingError(400,
                "No contract found for this college. Upload a contract first before generating invoices.");

        BigDecimal contractAmount = contract.getContractAmount();

        // ── Overlap check — reject if a non-cancelled invoice already covers this period ──
        if (CollegeInvoiceRepository.INSTANCE.existsForContractAndPeriod(contract.getId(), billingStart, billingEnd)) {
            throw new RoutingError(409,
                    "An invoice for this billing period (" + billingStart + " to " + billingEnd +
                    ") already exists for this contract. Cancel the existing invoice before raising a new one.");
        }

        // ── Generate invoice number: Invoice-{CODE}-{NNN} ────────
        int existingCount = CollegeInvoiceRepository.INSTANCE.countByCollege(collegeId);
        String seq = String.format("%03d", existingCount + 1);
        String invoiceNumber = "Invoice-" + college.getCode() + "-" + seq;

        // ── Compute due date (30 days from today) ─────────────────
        String today    = LocalDate.now().format(ISO);
        String dueDate  = LocalDate.now().plusDays(30).format(ISO);

        // ── Build InvoiceData ─────────────────────────────────────
        InvoiceService.InvoiceData invoiceData = new InvoiceService.InvoiceData();
        invoiceData.invoiceNumber     = invoiceNumber;
        invoiceData.collegeName       = college.getName();
        invoiceData.collegeCode       = college.getCode();
        invoiceData.collegeCity       = null;  // College entity stores cityId only
        invoiceData.collegeState      = null;  // College entity stores stateId only
        invoiceData.collegeEmail      = college.getContactEmail();
        invoiceData.amount            = contractAmount;
        invoiceData.contractAmount    = contractAmount;
        invoiceData.billingPeriodStart = billingStart;
        invoiceData.billingPeriodEnd  = billingEnd;
        invoiceData.description       = description;
        invoiceData.generatedDate     = today;
        invoiceData.dueDate           = dueDate;

        // ── Generate PDF bytes ────────────────────────────────────
        byte[] pdfBytes = InvoiceService.generatePdf(invoiceData);

        // ── Upload to S3 ──────────────────────────────────────────
        String fileName = invoiceNumber + ".pdf";
        String s3Key    = S3Service.upload("invoices", fileName, pdfBytes, "application/pdf");
        String fileUrl  = S3Service.getDownloadUrl(s3Key);

        // ── Save to DB ────────────────────────────────────────────
        CollegeInvoice invoice = new CollegeInvoice();
        invoice.setCollege(college);
        invoice.setContract(contract);
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setContractAmount(contractAmount);
        invoice.setBillingPeriodStart(billingStart);
        invoice.setBillingPeriodEnd(billingEnd);
        invoice.setAmount(contractAmount);
        invoice.setDescription(description != null && !description.isBlank() ? description : "Applyra Platform Service Fee");
        invoice.setStatus("DRAFT");
        invoice.setS3Key(s3Key);
        invoice.setFileUrl(fileUrl);
        invoice.setGeneratedBy(request.getUser());
        invoice.save();

        // ── Fire-and-forget: email invoice to college TPOs + super admins ─
        {
            final String fCollegeName = college.getName();
            final String fInvoiceNum  = invoiceNumber;
            final String fFileUrl     = fileUrl;
            final String fDueDate     = fmt(dueDate);
            final String fPeriod      = fmtPeriod(billingStart, billingEnd);
            final String fAmount      = formatInr(contractAmount);

            // Collect recipients: college contact email + TPO users + all super admins
            List<String> collegeEmails = new ArrayList<>();
            if (college.getContactEmail() != null && !college.getContactEmail().isBlank()) {
                collegeEmails.add(college.getContactEmail().trim().toLowerCase());
            }
            UserRepository.INSTANCE.byCollegeAndType(college.getId(), UserType.TPO)
                    .stream()
                    .filter(u -> u.getEmail() != null && !u.getEmail().isBlank())
                    .map(u -> u.getEmail().trim().toLowerCase())
                    .filter(e -> !collegeEmails.contains(e))
                    .forEach(collegeEmails::add);

            List<String> adminEmails = UserRepository.INSTANCE.findByUserType(UserType.SUPER_ADMIN)
                    .stream()
                    .filter(u -> u.getEmail() != null && !u.getEmail().isBlank())
                    .map(u -> u.getEmail().trim().toLowerCase())
                    .collect(Collectors.toList());

            new Thread(() -> {
                try {
                    // Email college contact + TPOs (invoice email)
                    String collegeHtml = EmailService.buildInvoiceEmailHtml(
                            fCollegeName, fInvoiceNum, fPeriod, fAmount, fFileUrl, fDueDate
                    );
                    String subject = "Invoice " + fInvoiceNum + " — Applyra Platform";
                    for (String email : collegeEmails) {
                        EmailService.sendEmail(email, subject, collegeHtml)
                                .subscribe(
                                        sent -> System.out.println("[Invoice] College email " + (sent ? "sent" : "failed") + " to " + email),
                                        err  -> System.err.println("[Invoice] College email error to " + email + ": " + err.getMessage())
                                );
                    }

                    // Email super admins (internal notification)
                    String adminHtml = EmailService.buildInvoiceEmailHtml(
                            fCollegeName, fInvoiceNum, fPeriod, fAmount, fFileUrl, fDueDate
                    );
                    String adminSubject = "[Admin] Invoice " + fInvoiceNum + " raised for " + fCollegeName;
                    for (String email : adminEmails) {
                        EmailService.sendEmail(email, adminSubject, adminHtml)
                                .subscribe(
                                        sent -> System.out.println("[Invoice] Admin email " + (sent ? "sent" : "failed") + " to " + email),
                                        err  -> System.err.println("[Invoice] Admin email error to " + email + ": " + err.getMessage())
                                );
                    }
                } catch (Exception e) {
                    System.err.println("[Invoice] Email thread error: " + e.getMessage());
                }
            }, "invoice-email").start();
        }

        // ── Build response ────────────────────────────────────────
        Map<String, Object> result = new HashMap<>();
        result.put("invoiceId",          invoice.getId());
        result.put("invoiceNumber",       invoiceNumber);
        result.put("contractAmount",      contractAmount);
        result.put("amount",              contractAmount);
        result.put("billingPeriodStart",  billingStart);
        result.put("billingPeriodEnd",    billingEnd);
        result.put("description",         invoice.getDescription());
        result.put("status",              "DRAFT");
        result.put("fileUrl",             fileUrl);
        result.put("s3Key",               s3Key);
        result.put("generatedDate",       today);
        result.put("dueDate",             dueDate);
        result.put("message",             "Invoice " + invoiceNumber + " generated and sent to " + college.getContactEmail());

        return result;
    }

    private static String fmt(String iso) {
        if (iso == null || iso.isEmpty()) return "";
        try { return LocalDate.parse(iso).format(DISPLAY); } catch (Exception e) { return iso; }
    }

    private static String fmtPeriod(String start, String end) {
        if (start == null && end == null) return "-";
        return fmt(start) + (end != null ? " – " + fmt(end) : "");
    }

    private static String formatInr(BigDecimal amount) {
        if (amount == null) return "₹0";
        NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        return nf.format(amount);
    }
}

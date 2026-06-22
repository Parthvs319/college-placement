package superadmin;

import helpers.annotations.SuperAdminRole;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.PasswordUtils;
import helpers.utils.ResponseUtils;
import io.ebean.DB;
import io.vertx.rxjava.ext.web.FileUpload;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.superadmin.SuperAdminAccessMiddleware;
import models.body.SuperAdminLoginRequest;
import models.enums.UserType;
import models.repos.CollegeContractRepository;
import models.repos.CollegeDocumentRepository;
import models.repos.CollegeRepository;
import models.repos.UserRepository;
import models.services.EmailService;
import models.services.S3Service;
import models.sql.*;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * POST /admin/colleges/:collegeId/contract
 *
 * Accepts multipart/form-data with:
 *   - file            (required) — the contract PDF
 *   - contractAmount  (required) — annual value in INR
 *   - validFrom       (optional) — ISO date "YYYY-MM-DD"
 *   - validTo         (optional) — ISO date "YYYY-MM-DD"
 *   - tpoEmail        (optional) — if provided, creates a TPO account and emails credentials
 *   - tpoName         (optional) — full name for the TPO account
 *   - contractLabel   (optional) — human-readable label, default "Applyra Contract 2025"
 *
 * Workflow:
 *   1. Upload contract PDF to S3 → contracts/{uuid}.pdf
 *   2. Save as CollegeDocument (documentType = APPLYRA_CONTRACT)
 *   3. Create CollegeContract record
 *   4. Optionally create TPO User account + send onboarding email with contract link + credentials
 */
@SuperAdminRole
public enum UploadContractController implements BaseController {

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

        // ── Read form fields ──────────────────────────────────────
        String contractAmountStr = rc.request().formAttributes().get("contractAmount");
        String validFrom         = rc.request().formAttributes().get("validFrom");
        String validTo           = rc.request().formAttributes().get("validTo");
        String contractLabel     = rc.request().formAttributes().get("contractLabel");
        String contractTypeRaw   = rc.request().formAttributes().get("contractType");

        // TPO details come from the college's own verified contact details
        String tpoEmail = college.getContactEmail();
        String tpoName  = college.getTpoName() != null && !college.getTpoName().isBlank()
                ? college.getTpoName() : college.getName() + " Placement Officer";

        String contractType = (contractTypeRaw != null && contractTypeRaw.equalsIgnoreCase("FREE_TRIAL"))
                ? "FREE_TRIAL" : "PAID";
        boolean isFreeTrial = "FREE_TRIAL".equals(contractType);

        // Amount: required for PAID; defaults to 0 for FREE_TRIAL
        BigDecimal contractAmount;
        if (contractAmountStr != null && !contractAmountStr.isBlank()) {
            try {
                contractAmount = new BigDecimal(contractAmountStr.trim());
            } catch (NumberFormatException e) {
                throw new RoutingError("Invalid contractAmount — must be a number");
            }
        } else if (isFreeTrial) {
            contractAmount = BigDecimal.ZERO;
        } else {
            throw new RoutingError("contractAmount is required for paid contracts");
        }

        // Default label
        if (contractLabel == null || contractLabel.isBlank()) {
            contractLabel = "Applyra " + (isFreeTrial ? "Free Trial" : "Contract") + " " + LocalDate.now().getYear();
        }

        // ── Read uploaded file (optional for FREE_TRIAL) ──────────
        List<FileUpload> uploads = rc.fileUploads();
        boolean hasFile = uploads != null && !uploads.isEmpty() && uploads.get(0).size() > 0;

        if (!hasFile && !isFreeTrial) {
            throw new RoutingError("Contract PDF is required for paid contracts.");
        }

        String s3Key   = null;
        String fileUrl = null;
        byte[] fileBytes   = null;
        String pdfFileName = null;
        CollegeDocument doc = null;

        if (hasFile) {
            FileUpload fu = uploads.get(0);
            if (fu.size() > 20 * 1024 * 1024) throw new RoutingError("File too large — max 20 MB");

            String contentType = fu.contentType();
            try {
                fileBytes = Files.readAllBytes(Paths.get(fu.uploadedFileName()));
            } catch (Exception e) {
                throw new RoutingError("Failed to read uploaded contract file: " + e.getMessage());
            }

            // Upload to S3
            s3Key   = S3Service.upload("contracts", fu.fileName(), fileBytes, contentType);
            fileUrl = S3Service.getDownloadUrl(s3Key);
            pdfFileName = fu.fileName();

            // Save CollegeDocument
            doc = new CollegeDocument();
            doc.setCollege(college);
            doc.setDocumentType("APPLYRA_CONTRACT");
            doc.setLabel(contractLabel);
            doc.setFileName(fu.fileName());
            doc.setFileUrl(fileUrl);
            doc.setContentType(contentType);
            doc.setFileSizeBytes(fu.size());
            if (validTo != null && !validTo.isBlank()) doc.setExpiryDate(validTo);
            doc.save();
        }

        // ── Save CollegeContract ──────────────────────────────────
        CollegeContract contract = new CollegeContract();
        contract.setCollege(college);
        if (doc != null) contract.setDocument(doc);
        contract.setContractAmount(contractAmount);
        contract.setContractType(contractType);
        contract.setValidFrom(validFrom != null && !validFrom.isBlank() ? validFrom : null);
        contract.setValidTo(validTo != null && !validTo.isBlank() ? validTo : null);
        contract.setStatus("ACTIVE");
        contract.save();

        // ── Optionally create TPO account + send email ────────────
        String generatedPassword = null;
        User tpoUser = null;

        if (tpoEmail != null && !tpoEmail.isBlank()) {
            tpoEmail = tpoEmail.trim().toLowerCase();

            // Check if user already exists
            User existing = UserRepository.INSTANCE.byEmail(tpoEmail);
            if (existing != null) {
                // Link existing user to this contract if they belong to the same college
                if (existing.getCollege() != null && existing.getCollege().getId().equals(collegeId)) {
                    tpoUser = existing;
                }
                // Don't overwrite — just note we found an existing user
            } else {
                // Create new TPO account
                generatedPassword = generatePassword();
                tpoUser = new User();
                tpoUser.setEmail(tpoEmail);
                tpoUser.setName(tpoName != null && !tpoName.isBlank() ? tpoName.trim() : "TPO");
                tpoUser.setPassword(PasswordUtils.INSTANCE.hash(generatedPassword));
                tpoUser.setUserType(UserType.TPO);
                tpoUser.setCollege(college);
                tpoUser.setVerified(true);
                tpoUser.setActive(true);
                tpoUser.save();
            }

            // Link TPO to contract
            if (tpoUser != null) {
                contract.setTpoUser(tpoUser);
                contract.save();
            }

            // Send onboarding email (fire-and-forget on a background thread)
            final String fTpoEmail      = tpoEmail;
            final String fTpoName       = tpoName != null && !tpoName.isBlank() ? tpoName.trim() : "Placement Officer";
            final String fPassword      = generatedPassword;
            final String fContractUrl   = fileUrl;
            final String fCollegeName   = college.getName();
            final String fCollegeCode   = college.getCode();
            final String fValidFrom     = validFrom;
            final String fValidTo       = validTo;
            final String fContractType  = contractType;
            final String fPdfFileName   = pdfFileName;
            final byte[] fFileBytes     = fileBytes;
            final String fAmountDisplay = isFreeTrial ? "Free Trial"
                    : "₹" + contractAmount.toPlainString();
            final String portalUrl      = System.getenv().getOrDefault("PORTAL_URL", "https://applyra.in");

            new Thread(() -> {
                try {
                    String html = EmailService.buildContractWithCredentialsHtml(
                            fTpoName, fCollegeName, fCollegeCode,
                            fTpoEmail,
                            fPassword != null ? fPassword : "(existing account — use your current password)",
                            fContractUrl, fValidFrom, fValidTo, portalUrl,
                            fAmountDisplay, fContractType
                    );
                    String subject = "Applyra " + (isFreeTrial ? "Free Trial" : "Contract")
                            + " & TPO Credentials — " + fCollegeName;

                    // Attach PDF if available
                    if (fFileBytes != null && fPdfFileName != null) {
                        EmailService.sendEmailWithAttachment(fTpoEmail, subject, html, fFileBytes, fPdfFileName)
                                .subscribe(
                                        sent -> System.out.println("[Contract] Email+attachment " + (sent ? "sent" : "failed") + " to " + fTpoEmail),
                                        err  -> System.err.println("[Contract] Email error: " + err.getMessage())
                                );
                    } else {
                        EmailService.sendEmail(fTpoEmail, subject, html)
                                .subscribe(
                                        sent -> System.out.println("[Contract] Email " + (sent ? "sent" : "failed") + " to " + fTpoEmail),
                                        err  -> System.err.println("[Contract] Email error: " + err.getMessage())
                                );
                    }
                } catch (Exception e) {
                    System.err.println("[Contract] Email thread error: " + e.getMessage());
                }
            }, "contract-email").start();
        }

        // ── Build response ────────────────────────────────────────
        Map<String, Object> result = new HashMap<>();
        result.put("contractId",    contract.getId());
        result.put("contractType",  contractType);
        if (doc != null)    result.put("documentId", doc.getId());
        if (s3Key != null)  result.put("s3Key",      s3Key);
        if (fileUrl != null) result.put("fileUrl",   fileUrl);
        if (pdfFileName != null) result.put("fileName", pdfFileName);
        result.put("contractAmount", contractAmount);
        result.put("validFrom",   contract.getValidFrom());
        result.put("validTo",     contract.getValidTo());
        result.put("status",      contract.getStatus());
        result.put("tpoCreated",  generatedPassword != null);
        if (tpoUser != null) {
            result.put("tpoEmail", tpoUser.getEmail());
            result.put("tpoName",  tpoUser.getName());
        }
        result.put("message", "Contract saved successfully" +
                (generatedPassword != null ? " and TPO account created — credentials emailed to " + tpoEmail : ""));

        return result;
    }

    /** Generates a secure random 12-character password (letters + digits) */
    private static String generatePassword() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789@#$!";
        StringBuilder sb = new StringBuilder(12);
        java.util.Random rnd = new java.security.SecureRandom();
        for (int i = 0; i < 12; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return sb.toString();
    }
}

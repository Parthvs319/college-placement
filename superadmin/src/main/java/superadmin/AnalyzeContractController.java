package superadmin;

import helpers.annotations.SuperAdminRole;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.FileUpload;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.superadmin.SuperAdminAccessMiddleware;
import models.body.SuperAdminLoginRequest;
import models.services.ContractTextExtractor;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * POST /admin/colleges/:collegeId/analyze-contract
 *
 * Accepts a multipart PDF upload, runs text extraction (PDFBox → Textract fallback),
 * parses contract fields using regex, and returns them for frontend prefilling.
 *
 * Does NOT save anything to the DB or S3. Read-only analysis endpoint.
 *
 * Request:  multipart/form-data with field "file" (PDF)
 * Response: { contractAmount, validFrom, validTo, tpoEmail, tpoName,
 *              extractionMethod, confidence, rawTextPreview }
 */
@SuperAdminRole
public enum AnalyzeContractController implements BaseController {

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
        List<FileUpload> uploads = rc.fileUploads();
        if (uploads == null || uploads.isEmpty()) {
            throw new RoutingError("No file uploaded. Send multipart/form-data with a 'file' field.");
        }

        FileUpload fu = uploads.get(0);
        if (fu.size() == 0)                    throw new RoutingError("Uploaded file is empty");
        if (fu.size() > 20 * 1024 * 1024)     throw new RoutingError("File too large — max 20 MB");

        byte[] bytes;
        try {
            bytes = Files.readAllBytes(Paths.get(fu.uploadedFileName()));
        } catch (Exception e) {
            throw new RoutingError("Failed to read uploaded file: " + e.getMessage());
        }

        // ── Extract text + parse fields ───────────────────────────────
        ContractTextExtractor.ContractExtractResult result = ContractTextExtractor.extract(bytes);

        // Raw text preview (first 1000 chars) for debugging
        String preview = "";
        if (result.rawText != null && !result.rawText.isBlank()) {
            preview = result.rawText.length() > 1000
                    ? result.rawText.substring(0, 1000).trim() + "…"
                    : result.rawText.trim();
        }

        // ── Build response ────────────────────────────────────────────
        Map<String, Object> out = new HashMap<>();
        out.put("contractAmount",    result.contractAmount);
        out.put("validFrom",         result.validFrom);
        out.put("validTo",           result.validTo);
        out.put("tpoEmail",          result.tpoEmail);
        out.put("tpoName",           result.tpoName);
        out.put("extractionMethod",  result.extractionMethod);
        out.put("confidence",        result.confidence);
        out.put("rawTextPreview",    preview);
        out.put("textLength",        result.rawText != null ? result.rawText.length() : 0);

        boolean anyFieldFound = result.contractAmount != null || result.validFrom != null
                || result.validTo != null || result.tpoEmail != null;
        out.put("success", anyFieldFound);
        out.put("message", anyFieldFound
                ? "Contract analyzed successfully via " + result.extractionMethod
                : "Could not extract fields. The PDF may be encrypted or image-only with Textract unavailable.");

        return out;
    }
}

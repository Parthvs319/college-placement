package superadmin;

import helpers.annotations.SuperAdminRole;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.superadmin.SuperAdminAccessMiddleware;
import models.body.SuperAdminLoginRequest;
import models.repos.CollegeContractRepository;
import models.repos.CollegeRepository;
import models.services.S3Service;
import models.sql.College;
import models.sql.CollegeContract;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GET /admin/colleges/:collegeId/contract
 *
 * Returns the latest active contract for a college, with a fresh pre-signed download URL.
 * Returns 404 if no contract exists.
 */
@SuperAdminRole
public enum GetContractController implements BaseController {

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

        // Try latest active first, fall back to latest of any status
        CollegeContract contract = CollegeContractRepository.INSTANCE.latestActive(collegeId);
        if (contract == null) {
            contract = CollegeContractRepository.INSTANCE.latest(collegeId);
        }
        if (contract == null) throw new RoutingError(404, "No contract found for this college");

        // Refresh pre-signed download URL
        String downloadUrl = null;
        if (contract.getDocument() != null) {
            String s3Key = contract.getDocument().getFileUrl();
            // If the stored URL is already a public URL, use it; otherwise generate a pre-signed one
            if (s3Key != null && !s3Key.startsWith("http")) {
                downloadUrl = S3Service.getDownloadUrl(s3Key);
            } else {
                downloadUrl = s3Key;
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("contractId",     contract.getId());
        result.put("contractNumber", contract.getContractNumber());
        result.put("contractAmount", contract.getContractAmount());
        result.put("contractType",   contract.getContractType());
        result.put("validFrom",      contract.getValidFrom());
        result.put("validTo",        contract.getValidTo());
        result.put("status",         contract.getStatus());
        result.put("notes",          contract.getNotes());
        result.put("createdAt",      contract.getCreatedAt() != null ? contract.getCreatedAt().toString() : null);

        if (contract.getDocument() != null) {
            result.put("fileName",    contract.getDocument().getFileName());
            result.put("fileUrl",     downloadUrl);
            result.put("contentType", contract.getDocument().getContentType());
            result.put("fileSizeBytes", contract.getDocument().getFileSizeBytes());
            result.put("documentId",  contract.getDocument().getId());
        }

        if (contract.getTpoUser() != null) {
            Map<String, Object> tpo = new HashMap<>();
            tpo.put("id",    contract.getTpoUser().getId());
            tpo.put("name",  contract.getTpoUser().getName());
            tpo.put("email", contract.getTpoUser().getEmail());
            result.put("tpoUser", tpo);
        }

        return result;
    }
}

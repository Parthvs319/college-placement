package college;

import helpers.annotations.CollegeRole;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.FileUpload;
import io.vertx.rxjava.ext.web.RoutingContext;
import lombok.Data;
import models.access.middlewear.college.CollegeAccessMiddleware;
import models.body.CollegeLoginRequest;
import models.services.S3Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@CollegeRole
public enum FileUploadController implements BaseController {

    INSTANCE;

    private static final Set<String> ALLOWED_FOLDERS = Set.of("resumes", "jd", "offers", "documents");
    private static final long MAX_SIZE = 10 * 1024 * 1024; // 10 MB

    @Override
    public void handle(RoutingContext event) {
        CollegeAccessMiddleware.INSTANCE.with(event, new ArrayList<>(), this.getClass())
                .map(req -> map(req, event))
                .subscribe(
                        o -> ResponseUtils.INSTANCE.writeJsonResponse(event, o),
                        error -> ResponseUtils.INSTANCE.handleError(event, error)
                );
    }

    private Object map(CollegeLoginRequest request, RoutingContext event) {

        List<FileUpload> uploads = event.fileUploads();
        if (uploads == null || uploads.isEmpty()) {
            throw new RoutingError("No file uploaded");
        }

        FileUpload file = uploads.iterator().next();
        String fileName = file.fileName();
        long fileSize = file.size();

        if (fileSize > MAX_SIZE) {
            throw new RoutingError("File too large. Maximum size is 10 MB.");
        }

        if (!S3Service.isAllowedType(fileName, ".pdf", ".doc", ".docx", ".png", ".jpg", ".jpeg", ".txt")) {
            throw new RoutingError("File type not allowed. Use PDF, DOC, DOCX, PNG, JPG, or TXT.");
        }

        String folder = "documents";
        if (request.getRequest().isPresent("folder")) {
            folder = request.getRequest().get("folder");
        }
        if (!ALLOWED_FOLDERS.contains(folder)) {
            folder = "documents";
        }

        try {
            byte[] data = Files.readAllBytes(Paths.get(file.uploadedFileName()));
            String key = S3Service.upload(folder, fileName, data, file.contentType());
            String url = S3Service.getPublicUrl(key);

            UploadResponse response = new UploadResponse();
            response.key = key;
            response.url = url;
            response.fileName = fileName;
            response.size = fileSize;
            response.contentType = file.contentType();
            return response;
        } catch (IOException e) {
            throw new RoutingError("Failed to read uploaded file: " + e.getMessage());
        }
    }

    @Data
    static class UploadResponse {
        String key;
        String url;
        String fileName;
        long size;
        String contentType;
    }
}

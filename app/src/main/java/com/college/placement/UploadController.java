package com.college.placement;

import helpers.annotations.UserAnnotation;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.FileUpload;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.user.UserAccessMiddleware;
import models.body.UserLoginRequest;
import models.services.OcrService;
import models.services.S3Service;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Generic file upload endpoint.
 *
 * POST /api/upload?folder=resumes|documents|logos
 *
 * Accepts multipart file upload, stores in S3, runs OCR (for PDFs/images),
 * and returns the S3 URL + extracted text in one synchronous response.
 *
 * Response:
 * {
 *   "s3Key": "resumes/uuid.pdf",
 *   "s3Url": "https://bucket.s3.region.amazonaws.com/resumes/uuid.pdf",
 *   "fileName": "original-name.pdf",
 *   "contentType": "application/pdf",
 *   "fileSize": 102400,
 *   "extractedText": "..." (null if not a document type)
 * }
 */
@UserAnnotation
public enum UploadController implements BaseController {
    INSTANCE;

    private static final Set<String> ALLOWED_FOLDERS = Set.of(
            "resumes", "documents", "logos", "offer-letters", "brochures", "jd"
    );

    private static final Set<String> OCR_CONTENT_TYPES = Set.of(
            "application/pdf",
            "image/png",
            "image/jpeg",
            "image/jpg",
            "image/tiff"
    );

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB

    @Override
    public void handle(RoutingContext event) {
        UserAccessMiddleware.INSTANCE.with(event, new ArrayList<>(), this.getClass())
                .map(req -> map(req, event))
                .subscribe(
                        o -> ResponseUtils.INSTANCE.writeJsonResponse(event, o),
                        error -> ResponseUtils.INSTANCE.handleError(event, error)
                );
    }

    private Object map(UserLoginRequest request, RoutingContext rc) {
        String folder = rc.queryParams().get("folder");
        if (folder == null || folder.isEmpty()) {
            folder = "uploads";
        }
        if (!ALLOWED_FOLDERS.contains(folder) && !folder.equals("uploads")) {
            throw new RoutingError("Invalid folder. Allowed: " + ALLOWED_FOLDERS);
        }

        // Get uploaded file
        List<FileUpload> uploads = rc.fileUploads();
        if (uploads == null || uploads.isEmpty()) {
            throw new RoutingError("No file uploaded. Send a multipart form with a 'file' field.");
        }

        FileUpload fileUpload = uploads.get(0);
        String originalFileName = fileUpload.fileName();
        String contentType = fileUpload.contentType();
        long fileSize = fileUpload.size();

        if (fileSize > MAX_FILE_SIZE) {
            throw new RoutingError("File too large. Maximum size is 10 MB.");
        }

        if (fileSize == 0) {
            throw new RoutingError("Empty file uploaded.");
        }

        // Read file bytes
        byte[] fileBytes;
        try {
            fileBytes = Files.readAllBytes(Paths.get(fileUpload.uploadedFileName()));
        } catch (Exception e) {
            throw new RoutingError("Failed to read uploaded file: " + e.getMessage());
        }

        // Upload to S3
        String s3Key = S3Service.upload(folder, originalFileName, fileBytes, contentType);
        String s3Url = S3Service.getPublicUrl(s3Key);

        // Run OCR synchronously for document types
        String extractedText = null;
        if (OCR_CONTENT_TYPES.contains(contentType)) {
            try {
                extractedText = OcrService.extractTextFromBytes(fileBytes);
            } catch (Exception e) {
                System.err.println("[Upload] OCR failed for " + s3Key + ": " + e.getMessage());
                // Don't fail the upload — OCR is best-effort
            }
        }

        // Build response
        Map<String, Object> response = new HashMap<>();
        response.put("s3Key", s3Key);
        response.put("s3Url", s3Url);
        response.put("fileName", originalFileName);
        response.put("contentType", contentType);
        response.put("fileSize", fileSize);
        response.put("extractedText", extractedText);

        System.out.println("[Upload] " + originalFileName + " → " + s3Key
                + " (" + fileSize + " bytes, OCR: " + (extractedText != null ? extractedText.length() + " chars" : "skipped") + ")");

        return response;
    }
}

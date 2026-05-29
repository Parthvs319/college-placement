package student;

import helpers.annotations.StudentRole;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import lombok.Data;
import models.access.middlewear.student.StudentAccessMiddleware;
import models.body.StudentLoginRequest;
import models.repos.ResumeRepository;
import models.services.RabbitMQService;
import models.sql.Resume;
import models.sql.Student;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;

/**
 * POST /student/me/resume
 *
 * Accepts pre-uploaded S3 file details from the generic /api/upload endpoint.
 * Body (JSON):
 * {
 *   "s3Key": "resumes/uuid.pdf",
 *   "s3Url": "https://...",
 *   "fileName": "my-resume.pdf",
 *   "contentType": "application/pdf",
 *   "fileSize": 102400,
 *   "extractedText": "...",
 *   "label": "SDE Resume"  (optional)
 * }
 */
@StudentRole
public enum ResumeUploadController implements BaseController {

    INSTANCE;

    private static final int MAX_RESUMES = 5;

    @Override
    public void handle(RoutingContext event) {
        StudentAccessMiddleware.INSTANCE.with(event, new ArrayList<>(), this.getClass())
                .map(this::map)
                .subscribe(
                        o -> ResponseUtils.INSTANCE.writeJsonResponse(event, o),
                        error -> ResponseUtils.INSTANCE.handleError(event, error)
                );
    }

    private Object map(StudentLoginRequest request) {
        Student student = request.getStudent();

        int existingCount = ResumeRepository.INSTANCE.countByStudent(student.getId());
        if (existingCount >= MAX_RESUMES) {
            throw new RoutingError("Maximum " + MAX_RESUMES + " resumes allowed. Delete an existing resume first.");
        }

        var body = request.getRequest();
        String s3Key = body.get("s3Key");
        String s3Url = body.get("s3Url");
        String fileName = body.get("fileName");
        String contentType = body.get("contentType");
        String fileSizeStr = body.get("fileSize");
        String extractedText = body.get("extractedText");
        String label = body.get("label");

        if (s3Key == null || s3Url == null || fileName == null) {
            throw new RoutingError("s3Key, s3Url, and fileName are required. Upload the file via /api/upload first.");
        }

        long fileSize = 0;
        if (fileSizeStr != null) {
            try {
                fileSize = Long.parseLong(fileSizeStr);
            } catch (NumberFormatException ignored) {}
        }

        boolean isPrimary = existingCount == 0;

        Resume resume = new Resume();
        resume.student = student;
        resume.fileName = fileName;
        resume.s3Key = s3Key;
        resume.url = s3Url;
        resume.contentType = contentType;
        resume.fileSize = fileSize;
        resume.primary = isPrimary;
        resume.label = label;
        resume.save();

        // Sync legacy field
        if (isPrimary) {
            student.resumeUrl = s3Url;
            student.update();
        }

        // If OCR text was provided by the upload API, queue ATS scoring directly
        if (extractedText != null && !extractedText.isEmpty()) {
            try {
                RabbitMQService.publish(RabbitMQService.Q_AI, "ATS_SCORE_BASIC",
                        new JsonObject()
                                .put("studentId", student.getId())
                                .put("resumeId", resume.getId())
                                .put("resumeText", extractedText));
            } catch (Exception e) {
                System.out.println("[ResumeUpload] Queue unavailable, ATS scoring skipped");
            }
        }

        ResumeResponse response = new ResumeResponse();
        response.id = resume.getId();
        response.s3Url = s3Url;
        response.s3Key = s3Key;
        response.fileName = fileName;
        response.fileSize = fileSize;
        response.primary = isPrimary;
        response.label = label;
        response.hasExtractedText = (extractedText != null && !extractedText.isEmpty());
        response.message = "Resume saved successfully." + (isPrimary ? " Marked as primary." : "")
                + (response.hasExtractedText ? " ATS scoring queued." : "");
        return response;
    }

    @Data
    static class ResumeResponse {
        Long id;
        String s3Url;
        String s3Key;
        String fileName;
        long fileSize;
        boolean primary;
        String label;
        boolean hasExtractedText;
        String message;
    }
}

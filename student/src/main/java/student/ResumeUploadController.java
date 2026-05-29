package student;

import helpers.annotations.StudentRole;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.FileUpload;
import io.vertx.rxjava.ext.web.RoutingContext;
import lombok.Data;
import models.access.middlewear.student.StudentAccessMiddleware;
import models.body.StudentLoginRequest;
import models.enums.UserType;
import models.repos.ResumeRepository;
import models.repos.StudentRepository;
import models.services.RabbitMQService;
import models.services.S3Service;
import models.sql.Resume;
import models.sql.Student;
import io.vertx.core.json.JsonObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;


@StudentRole
public enum ResumeUploadController implements BaseController {

    INSTANCE;

    private static final long MAX_SIZE = 5 * 1024 * 1024; // 5 MB
    private static final int MAX_RESUMES = 5;

    @Override
    public void handle(RoutingContext event) {
        StudentAccessMiddleware.INSTANCE.with(event, new ArrayList<>(), this.getClass())
                .map(req -> map(req, event))
                .subscribe(
                        o -> ResponseUtils.INSTANCE.writeJsonResponse(event, o),
                        error -> ResponseUtils.INSTANCE.handleError(event, error)
                );
    }

    private Object map(StudentLoginRequest request, RoutingContext event) {
        UserType userType = request.getUser().getUserType();
        if (!userType.equals(UserType.STUDENT)) {
            throw new RoutingError("Only students can upload resumes");
        }
        Student student = StudentRepository.INSTANCE.byUserId(request.getUser().getId());
        if (student == null) {
            throw new RoutingError("Student profile not found. Please complete onboarding first.");
        }

        // Check max resume limit
        int existingCount = ResumeRepository.INSTANCE.countByStudent(student.getId());
        if (existingCount >= MAX_RESUMES) {
            throw new RoutingError("Maximum " + MAX_RESUMES + " resumes allowed. Please delete an existing resume first.");
        }

        List<FileUpload> uploads = event.fileUploads();
        if (uploads == null || uploads.isEmpty()) {
            throw new RoutingError("No file uploaded");
        }
        FileUpload file = uploads.iterator().next();
        String fileName = file.fileName();
        long fileSize = file.size();
        if (fileSize > MAX_SIZE) {
            throw new RoutingError("Resume too large. Maximum size is 5 MB.");
        }
        if (!S3Service.isAllowedType(fileName, ".pdf", ".doc", ".docx")) {
            throw new RoutingError("Resume must be PDF or DOCX format.");
        }

        String label = event.request().getParam("label");

        try {
            byte[] data = Files.readAllBytes(Paths.get(file.uploadedFileName()));
            String key = S3Service.upload("resumes", fileName, data, file.contentType());
            String url = S3Service.getPublicUrl(key);

            boolean isPrimary = existingCount == 0;

            Resume resume = new Resume();
            resume.student = student;
            resume.fileName = fileName;
            resume.s3Key = key;
            resume.url = url;
            resume.contentType = file.contentType();
            resume.fileSize = fileSize;
            resume.primary = isPrimary;
            resume.label = label;
            resume.save();

            if (isPrimary) {
                student.resumeUrl = url;
                student.update();
            }

            try {
                RabbitMQService.publish(RabbitMQService.Q_DOCUMENTS, "OCR_RESUME",
                        new JsonObject()
                                .put("resumeId", resume.getId())
                                .put("s3Key", key)
                                .put("studentId", student.getId()));
            } catch (Exception e) {
                System.out.println("[ResumeUpload] Queue unavailable, OCR will be processed later");
            }
            ResumeUploadResponse response = new ResumeUploadResponse();
            response.id = resume.getId();
            response.resumeUrl = url;
            response.key = key;
            response.fileName = fileName;
            response.size = fileSize;
            response.primary = isPrimary;
            response.label = label;
            response.message = "Resume uploaded successfully." + (isPrimary ? " Marked as primary." : "")
                    + " ATS score will be calculated shortly.";
            return response;
        } catch (IOException e) {
            throw new RoutingError("Failed to read uploaded file: " + e.getMessage());
        }
    }

    @Data
    static class ResumeUploadResponse {
        Long id;
        String resumeUrl;
        String key;
        String fileName;
        long size;
        boolean primary;
        String label;
        String message;
    }
}

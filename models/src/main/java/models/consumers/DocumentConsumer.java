package models.consumers;

import io.vertx.core.json.JsonObject;
import models.repos.ResumeRepository;
import models.repos.StudentRepository;
import models.services.OcrService;
import models.services.RabbitMQService;
import models.sql.Resume;
import models.sql.Student;

public class DocumentConsumer {

    public static void register() {
        RabbitMQService.consume(RabbitMQService.Q_DOCUMENTS, 3, message -> {
            String jobType = message.getString("jobType");
            JsonObject data = message.getJsonObject("data");

            System.out.println("[DocumentConsumer] Processing: " + jobType);

            switch (jobType) {
                case "OCR_RESUME":
                    handleOcrResume(data);
                    break;
                case "OCR_JD":
                    handleOcrJd(data);
                    break;
                case "OCR_DOCUMENT":
                    handleOcrDocument(data);
                    break;
                default:
                    System.err.println("[DocumentConsumer] Unknown job type: " + jobType);
            }
        });
    }

    private static void handleOcrResume(JsonObject data) {
        Long studentId = data.getLong("studentId");
        Long resumeId = data.getLong("resumeId");
        String s3Key = data.getString("s3Key");

        String extractedText = OcrService.extractText(s3Key);
        if (extractedText == null || extractedText.isEmpty()) {
            System.err.println("[DocumentConsumer] OCR returned empty for resume: " + s3Key);
            return;
        }

        Student student = StudentRepository.INSTANCE.byId(studentId);
        if (student == null) return;

        // Publish to AI queue for ATS scoring (free basic scoring)
        RabbitMQService.publish(RabbitMQService.Q_AI, "ATS_SCORE_BASIC",
                new JsonObject()
                        .put("studentId", studentId)
                        .put("resumeId", resumeId)
                        .put("resumeText", extractedText)
        );

        System.out.println("[DocumentConsumer] Resume OCR complete for student " + studentId
                + ", resume " + resumeId + " (" + extractedText.length() + " chars). ATS scoring queued.");
    }

    private static void handleOcrJd(JsonObject data) {
        Long driveId = data.getLong("driveId");
        String s3Key = data.getString("s3Key");

        String extractedText = OcrService.extractText(s3Key);
        if (extractedText == null || extractedText.isEmpty()) {
            System.err.println("[DocumentConsumer] OCR returned empty for JD: " + s3Key);
            return;
        }

        // Update the drive's jobDescription with extracted text
        var drive = models.repos.DriveRepository.INSTANCE.byId(driveId);
        if (drive != null) {
            drive.jobDescription = extractedText;
            drive.update();
            System.out.println("[DocumentConsumer] JD OCR complete for drive " + driveId);
        }
    }

    private static void handleOcrDocument(JsonObject data) {
        Long documentId = data.getLong("documentId");
        String s3Key = data.getString("s3Key");

        String extractedText = OcrService.extractText(s3Key);
        System.out.println("[DocumentConsumer] Document OCR complete for doc " + documentId
                + " (" + (extractedText != null ? extractedText.length() : 0) + " chars)");
        // Could store extracted text in a searchable field on Document entity
    }
}

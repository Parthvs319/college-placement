package models.consumers;

import io.vertx.core.json.JsonObject;
import models.repos.ResumeRepository;
import models.repos.StudentRepository;
import models.services.AIService;
import models.services.RabbitMQService;
import models.sql.Resume;
import models.sql.Student;

/**
 * Consumes AI processing jobs from placement.ai queue.
 *
 * Job types:
 *   ATS_SCORE_BASIC    — { studentId, resumeText } → calculate and store ATS score (free)
 *   ATS_SCORE_DETAILED — { studentId, resumeText } → detailed ATS report (premium)
 *   JD_MATCH           — { studentId, driveId, resumeText, jdText } → match score (premium)
 *   RESUME_IMPROVE     — { studentId, resumeText, targetRole } → suggestions (premium)
 *   RESUME_GENERATE    — { studentId, profileData } → generate ATS resume (premium)
 *   RESUME_PARSE       — { studentId, resumeText } → auto-fill profile (premium)
 */
public class AIConsumer {

    public static void register() {
        RabbitMQService.consume(RabbitMQService.Q_AI, 2, message -> {
            String jobType = message.getString("jobType");
            JsonObject data = message.getJsonObject("data");

            System.out.println("[AIConsumer] Processing: " + jobType);

            switch (jobType) {
                case "ATS_SCORE_BASIC":
                    handleAtsScoreBasic(data);
                    break;
                case "ATS_SCORE_DETAILED":
                case "JD_MATCH":
                case "RESUME_IMPROVE":
                case "RESUME_GENERATE":
                case "RESUME_PARSE":
                    // Premium jobs are processed on-demand via API, not async
                    // This handler is for background/batch processing
                    System.out.println("[AIConsumer] Premium job " + jobType
                            + " — processed on-demand via API controller");
                    break;
                default:
                    System.err.println("[AIConsumer] Unknown job type: " + jobType);
            }
        });
    }

    /**
     * Basic ATS scoring — runs automatically after resume OCR (free for all students).
     * Updates the Resume.atsScore and syncs to Student.atsScore if the resume is primary.
     */
    private static void handleAtsScoreBasic(JsonObject data) {
        Long studentId = data.getLong("studentId");
        Long resumeId = data.getLong("resumeId");
        String resumeText = data.getString("resumeText");

        if (resumeText == null || resumeText.isEmpty()) {
            System.err.println("[AIConsumer] No resume text for student " + studentId);
            return;
        }

        JsonObject result = AIService.calculateAtsScore(resumeText);
        Integer score = result.getInteger("score");

        if (score != null) {
            // Store score on the Resume entity
            if (resumeId != null) {
                Resume resume = ResumeRepository.INSTANCE.byId(resumeId);
                if (resume != null) {
                    resume.atsScore = score;
                    resume.update();
                    System.out.println("[AIConsumer] ATS score updated for resume "
                            + resumeId + ": " + score);

                    // Sync to student.atsScore if this is the primary resume
                    if (resume.primary) {
                        Student student = StudentRepository.INSTANCE.byId(studentId);
                        if (student != null) {
                            student.atsScore = score;
                            student.update();
                        }
                    }
                    return;
                }
            }

            // Fallback: legacy path without resumeId
            Student student = StudentRepository.INSTANCE.byId(studentId);
            if (student != null) {
                student.atsScore = score;
                student.update();
                System.out.println("[AIConsumer] ATS score updated for student "
                        + studentId + ": " + score);
            }
        }
    }
}

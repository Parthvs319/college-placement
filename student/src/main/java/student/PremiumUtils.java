package student;

import helpers.customErrors.RoutingError;
import models.body.StudentLoginRequest;
import models.repos.ResumeRepository;
import models.repos.SubscriptionRepository;
import models.services.OcrService;
import models.services.S3Service;
import models.sql.Resume;
import models.sql.Student;
import models.sql.Subscription;

/**
 * Shared utilities for premium AI feature controllers.
 * Handles subscription validation and resume text extraction.
 */
public final class PremiumUtils {

    private PremiumUtils() {}

    /**
     * Validate that the request is from a student with an active premium subscription.
     * Returns the Student entity if valid, throws RoutingError otherwise.
     */
    public static Student getVerifiedPremiumStudent(StudentLoginRequest request) {
        Student student = request.getStudent();
        if (student == null) {
            throw new RoutingError("Student profile not found");
        }

        // Check student-level subscription first, then college-level
        Subscription sub = SubscriptionRepository.INSTANCE.activeByStudent(student.getId());
        if (sub == null) {
            sub = SubscriptionRepository.INSTANCE.activeByCollege(student.college.getId());
        }
        if (sub == null) {
            throw new RoutingError("Premium subscription required. Upgrade to access AI features.");
        }

        // Check credits
        if (!sub.hasCredits()) {
            throw new RoutingError("Monthly AI credits exhausted. Credits reset next month.");
        }

        // Consume one credit
        sub.useCredit();

        return student;
    }

    /**
     * Extract text from the student's primary resume via OCR.
     * Uses the Resume entity (primary resume) first, falls back to legacy resumeUrl.
     * Throws RoutingError if no resume is found or text extraction fails.
     */
    public static String getResumeText(Student student) {
        // Try primary resume from Resume entity first
        Resume primaryResume = ResumeRepository.INSTANCE.primaryByStudent(student.getId());
        String s3Key = null;

        if (primaryResume != null) {
            s3Key = primaryResume.s3Key;
        } else if (student.resumeUrl != null && !student.resumeUrl.isEmpty()) {
            // Fall back to legacy resumeUrl
            s3Key = student.resumeUrl;
        } else {
            throw new RoutingError("Please upload your resume first");
        }

        String resumeText;
        if (s3Key.startsWith("resumes/")) {
            resumeText = OcrService.extractText(s3Key);
        } else {
            try {
                byte[] data = S3Service.download(s3Key);
                resumeText = OcrService.extractTextFromBytes(data);
            } catch (Exception e) {
                throw new RoutingError("Could not process resume. Please re-upload.");
            }
        }

        if (resumeText == null || resumeText.isEmpty()) {
            throw new RoutingError("Could not extract text from resume. Please upload a clear PDF.");
        }

        return resumeText;
    }
}

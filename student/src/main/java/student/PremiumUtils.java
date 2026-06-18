package student;

import helpers.customErrors.RoutingError;
import models.body.StudentLoginRequest;
import models.enums.CreditTransactionType;
import models.repos.ResumeRepository;
import models.services.CreditService;
import models.services.OcrService;
import models.services.S3Service;
import models.sql.Resume;
import models.sql.Student;

/**
 * Shared utilities for premium AI feature controllers.
 * Handles subscription validation and resume text extraction.
 */
public final class PremiumUtils {

    private PremiumUtils() {}

    /**
     * Validate that the request is from a student with an active premium subscription.
     * Consumes one credit and logs the transaction.
     * Returns the Student entity if valid, throws RoutingError otherwise.
     */
    public static Student getVerifiedPremiumStudent(StudentLoginRequest request,
                                                     CreditTransactionType featureType,
                                                     String description) {
        Student student = request.getStudent();
        if (student == null) {
            throw new RoutingError("Student profile not found");
        }

        // CreditService handles subscription lookup, credit check, deduction, and logging
        CreditService.INSTANCE.consumeCredit(student, featureType, description);

        return student;
    }

    /**
     * Overload for backward compatibility - uses AI_OTHER as default type.
     */
    public static Student getVerifiedPremiumStudent(StudentLoginRequest request) {
        return getVerifiedPremiumStudent(request, CreditTransactionType.AI_OTHER, "AI feature usage");
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

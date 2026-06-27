package college.student;

import helpers.annotations.CollegeRole;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.college.CollegeAccessMiddleware;
import models.body.CollegeLoginRequest;
import models.repos.StudentDocumentRepository;
import models.repos.StudentRepository;
import models.sql.Student;
import models.sql.StudentDocument;
import models.json.CollegeDtos;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * POST /college/students/:studentId/documents/:docId/verify
 *
 * Body (optional):
 * {
 *   "note": "Looks good"
 * }
 *
 * Verifies the document. After verification, checks if all required documents
 * are now verified and auto-verifies the student (user.verified = true) if so.
 *
 * Required documents for student verification:
 *   - COLLEGE_ID  (at least 1 verified)
 *   - MARKSHEET   (at least 1 verified)
 *   - AADHAR_CARD (at least 1 verified)
 *   - PAN_CARD    (at least 1 verified)
 */
@CollegeRole
public enum VerifyStudentDocumentController implements BaseController {

    INSTANCE;

    private static final Set<String> REQUIRED_TYPES = Set.of(
            "COLLEGE_ID", "MARKSHEET", "AADHAR_CARD", "PAN_CARD"
    );

    @Override
    public void handle(RoutingContext event) {
        CollegeAccessMiddleware.INSTANCE.with(event, new ArrayList<>(), this.getClass())
                .map(req -> map(req, event))
                .subscribe(
                        o -> ResponseUtils.INSTANCE.writeJsonResponse(event, o),
                        error -> ResponseUtils.INSTANCE.handleError(event, error)
                );
    }

    private Object map(CollegeLoginRequest request, RoutingContext rc) {
        Long collegeId = request.getCollege().getId();
        Long studentId = Long.parseLong(rc.pathParam("studentId"));
        Long docId     = Long.parseLong(rc.pathParam("docId"));

        Student student = StudentRepository.INSTANCE.byId(studentId);
        if (student == null || !student.getCollege().getId().equals(collegeId)) {
            throw new RoutingError("Student not found");
        }

        StudentDocument doc = StudentDocumentRepository.INSTANCE.byId(docId);
        if (doc == null || !doc.getStudent().getId().equals(studentId)) {
            throw new RoutingError("Document not found");
        }

        String note = request.getRequest().isPresent("note") ? request.getRequest().get("note") : null;
        doc.setVerified(true);
        doc.setVerificationNote(note != null ? note : "Verified by TPO");
        doc.save();

        // Auto-verify student if all required documents are verified
        checkAndAutoVerifyStudent(student, studentId);

        return CollegeDtos.toStudentDocumentDto(doc);
    }

    /**
     * Checks if all 4 required document types have at least 1 verified document.
     * If yes, sets student's user.verified = true.
     */
    static void checkAndAutoVerifyStudent(Student student, Long studentId) {
        List<StudentDocument> allDocs = StudentDocumentRepository.INSTANCE.byStudentId(studentId);

        Set<String> verifiedTypes = new java.util.HashSet<>();
        for (StudentDocument d : allDocs) {
            if (d.isVerified()) {
                verifiedTypes.add(d.getDocumentType());
            }
        }

        boolean allRequired = verifiedTypes.containsAll(REQUIRED_TYPES);
        if (allRequired && student.getUser() != null && !student.getUser().isVerified()) {
            student.getUser().setVerified(true);
            student.getUser().save();
            System.out.println("[DocVerify] Student " + studentId + " auto-verified — all required documents approved.");
        }
    }
}

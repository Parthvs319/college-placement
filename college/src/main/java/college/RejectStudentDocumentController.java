package college;

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
import student.StudentDtos;

import java.util.ArrayList;

/**
 * POST /college/students/:studentId/documents/:docId/reject
 *
 * Body (optional):
 * {
 *   "note": "Image is blurry, please re-upload"
 * }
 *
 * Marks the document as not verified. Student will need to re-upload.
 */
@CollegeRole
public enum RejectStudentDocumentController implements BaseController {

    INSTANCE;

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

        String note = request.getRequest().isPresent("note") ? request.getRequest().get("note") : "Rejected by TPO — please re-upload";
        doc.setVerified(false);
        doc.setVerificationNote(note);
        doc.save();

        return StudentDtos.toDocumentDto(doc);
    }
}

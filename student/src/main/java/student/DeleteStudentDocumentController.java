package student;

import helpers.annotations.StudentAnnotation;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.student.StudentAccessMiddleware;
import models.body.StudentLoginRequest;
import models.repos.StudentDocumentRepository;
import models.sql.StudentDocument;

import java.util.ArrayList;
import java.util.Map;

/**
 * DELETE /student/me/documents/:docId
 *
 * Soft-deletes a student document. Students can only delete their own documents.
 */
@StudentAnnotation
public enum DeleteStudentDocumentController implements BaseController {

    INSTANCE;

    @Override
    public void handle(RoutingContext event) {
        StudentAccessMiddleware.INSTANCE.with(event, new ArrayList<>(), this.getClass())
                .map(req -> map(req, event))
                .subscribe(
                        o -> ResponseUtils.INSTANCE.writeJsonResponse(event, o),
                        error -> ResponseUtils.INSTANCE.handleError(event, error)
                );
    }

    private Object map(StudentLoginRequest request, RoutingContext rc) {
        Long studentId = request.getStudent().getId();
        Long docId = Long.parseLong(rc.pathParam("docId"));

        StudentDocument doc = StudentDocumentRepository.INSTANCE.byId(docId);
        if (doc == null) {
            throw new RoutingError("Document not found");
        }
        if (!doc.getStudent().getId().equals(studentId)) {
            throw new RoutingError("Access denied");
        }

        doc.setDeleted(true);
        doc.save();

        return Map.of("message", "Document deleted");
    }
}

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
import student.StudentDtos;

import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * GET /college/students/:studentId/documents
 *
 * Returns all documents uploaded by a student (TPO view).
 */
@CollegeRole
public enum GetStudentDocumentsController implements BaseController {

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

        Student student = StudentRepository.INSTANCE.byId(studentId);
        if (student == null || !student.getCollege().getId().equals(collegeId)) {
            throw new RoutingError("Student not found");
        }

        return StudentDocumentRepository.INSTANCE
                .byStudentId(studentId)
                .stream()
                .map(StudentDtos::toDocumentDto)
                .collect(Collectors.toList());
    }
}

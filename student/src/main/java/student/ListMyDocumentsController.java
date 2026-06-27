package student;

import helpers.annotations.StudentRole;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.student.StudentAccessMiddleware;
import models.body.StudentLoginRequest;
import models.repos.StudentDocumentRepository;

import java.util.ArrayList;
import java.util.stream.Collectors;

@StudentRole
public enum ListMyDocumentsController implements BaseController {

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

        // Optional ?type= filter
        String typeParam = rc.queryParams().get("type");
        if (typeParam != null && !typeParam.isEmpty()) {
            return StudentDocumentRepository.INSTANCE
                    .byStudentIdAndType(studentId, typeParam.toUpperCase())
                    .stream()
                    .map(StudentDtos::toDocumentDto)
                    .collect(Collectors.toList());
        }

        return StudentDocumentRepository.INSTANCE
                .byStudentId(studentId)
                .stream()
                .map(StudentDtos::toDocumentDto)
                .collect(Collectors.toList());
    }
}

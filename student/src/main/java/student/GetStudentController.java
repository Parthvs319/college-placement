package student;

import helpers.annotations.UserAnnotation;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.user.UserAccessMiddleware;
import models.body.UserLoginRequest;
import models.repos.StudentRepository;
import models.sql.Student;

import java.util.ArrayList;

@UserAnnotation
public enum GetStudentController implements BaseController {

    INSTANCE;

    @Override
    public void handle(RoutingContext event) {
        UserAccessMiddleware.INSTANCE.with(event, new ArrayList<>(), this.getClass())
                .map(this::map)
                .subscribe(
                        o -> ResponseUtils.INSTANCE.writeJsonResponse(event, o),
                        error -> ResponseUtils.INSTANCE.handleError(event, error)
                );
    }

    private Object map(UserLoginRequest request) {
        String idParam = request.getRoutingContext().pathParam("id");
        Student student = StudentRepository.INSTANCE.byId(Long.parseLong(idParam));
        if (student == null) {
            throw new RoutingError("Student not found");
        }
        return student;
    }
}

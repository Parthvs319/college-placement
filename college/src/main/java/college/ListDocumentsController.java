package college;

import helpers.annotations.CollegeRole;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.college.CollegeAccessMiddleware;
import models.body.CollegeLoginRequest;
import models.json.CollegeDtos;
import models.repos.DocumentRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@CollegeRole
public enum ListDocumentsController implements BaseController {

    INSTANCE;

    @Override
    public void handle(RoutingContext event) {
        CollegeAccessMiddleware.INSTANCE.with(event, new ArrayList<>(), this.getClass())
                .map(this::map)
                .subscribe(
                        o -> ResponseUtils.INSTANCE.writeJsonResponse(event, o),
                        error -> ResponseUtils.INSTANCE.handleError(event, error)
                );
    }

    private Object map(CollegeLoginRequest request) {
        Long collegeId = request.getCollege().getId();

        // Optional type filter
        List<String> typeParam = request.getRoutingContext().queryParam("type");
        if (!typeParam.isEmpty()) {
            return DocumentRepository.INSTANCE.byCollegeAndType(collegeId, typeParam.get(0)).stream().map(CollegeDtos::toDocumentDto).collect(Collectors.toList());
        }

        return DocumentRepository.INSTANCE.byCollege(collegeId).stream().map(CollegeDtos::toDocumentDto).collect(Collectors.toList());
    }
}

package student;

import helpers.annotations.StudentRole;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.student.StudentAccessMiddleware;
import models.body.StudentLoginRequest;
import models.enums.RoundType;
import models.repos.PYQRepository;
import models.sql.PYQ;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@StudentRole
public enum GetPYQController implements BaseController {

    INSTANCE;

    @Override
    public void handle(RoutingContext event) {
        StudentAccessMiddleware.INSTANCE.with(event, new ArrayList<>(), this.getClass())
                .map(this::map)
                .subscribe(
                        o -> ResponseUtils.INSTANCE.writeJsonResponse(event, o),
                        error -> ResponseUtils.INSTANCE.handleError(event, error)
                );
    }

    private Object map(StudentLoginRequest request) {
        String companyIdParam = request.getRoutingContext().pathParam("companyId");
        Long companyId = Long.parseLong(companyIdParam);
        List<String> roundTypeParam = request.getRoutingContext().queryParam("roundType");
        List<PYQ> pyqs;
        if (!roundTypeParam.isEmpty()) {
            RoundType roundType = RoundType.valueOf(roundTypeParam.get(0));
            pyqs = PYQRepository.INSTANCE.byCompanyAndRoundType(companyId, roundType);
        } else {
            pyqs = PYQRepository.INSTANCE.byCompany(companyId);
        }
        return pyqs.stream().map(StudentDtos::toPyqDto).collect(Collectors.toList());
    }
}

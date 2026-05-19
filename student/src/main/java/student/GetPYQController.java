package student;

import helpers.annotations.UserAnnotation;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.user.UserAccessMiddleware;
import models.body.UserLoginRequest;
import models.enums.RoundType;
import models.repos.PYQRepository;

import java.util.ArrayList;
import java.util.List;

@UserAnnotation
public enum GetPYQController implements BaseController {

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
        String companyIdParam = request.getRoutingContext().pathParam("companyId");
        Long companyId = Long.parseLong(companyIdParam);

        // Optional filters
        List<String> roundTypeParam = request.getRoutingContext().queryParam("roundType");
        if (!roundTypeParam.isEmpty()) {
            RoundType roundType = RoundType.valueOf(roundTypeParam.get(0));
            return PYQRepository.INSTANCE.byCompanyAndRoundType(companyId, roundType);
        }

        return PYQRepository.INSTANCE.byCompany(companyId);
    }
}

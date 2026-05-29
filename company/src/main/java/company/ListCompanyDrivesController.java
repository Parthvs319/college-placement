package company;

import helpers.annotations.CompanyRole;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.company.CompanyAccessMiddleware;
import models.body.CompanyLoginRequest;
import models.repos.DriveRepository;

import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * Company HR views drives associated with their company (across all linked colleges).
 */
@CompanyRole
public enum ListCompanyDrivesController implements BaseController {

    INSTANCE;

    @Override
    public void handle(RoutingContext event) {
        CompanyAccessMiddleware.INSTANCE.with(event, new ArrayList<>(), this.getClass())
                .map(this::map)
                .subscribe(
                        o -> ResponseUtils.INSTANCE.writeJsonResponse(event, o),
                        error -> ResponseUtils.INSTANCE.handleError(event, error)
                );
    }

    private Object map(CompanyLoginRequest request) {
        Long companyId = Long.parseLong(request.getRoutingContext().pathParam("companyId"));
        return DriveRepository.INSTANCE.byCompany(companyId).stream()
                .map(CompanyDtos::toCompanyDriveDto).collect(Collectors.toList());
    }
}

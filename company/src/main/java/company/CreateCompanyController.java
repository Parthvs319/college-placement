package company;

import helpers.annotations.UserAnnotation;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.Request;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.user.UserAccessMiddleware;
import models.body.UserLoginRequest;
import models.enums.UserType;
import models.repos.CompanyRepository;
import models.sql.Company;

import java.util.ArrayList;

@UserAnnotation
public enum CreateCompanyController implements BaseController {

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
        UserType userType = request.getUser().getUserType();
        if (!userType.equals(UserType.COLLEGE_ADMIN) && !userType.equals(UserType.TPO) && !userType.equals(UserType.SUPER_ADMIN)) {
            throw new RoutingError("Not authorized to create companies");
        }

        Request body = request.getRequest();
        String name = body.get("name");
        if (name == null) {
            throw new RoutingError("Company name is required");
        }

        // Check if company with same name exists
        Company existing = CompanyRepository.INSTANCE.byName(name);
        if (existing != null) {
            throw new RoutingError("Company '" + name + "' already exists");
        }

        Company company = new Company();
        company.name = name;
        company.website = body.get("website");
        company.logoUrl = body.get("logoUrl");
        company.industry = body.get("industry");
        company.description = body.get("description");
        company.headquarters = body.get("headquarters");
        company.save();

        return CompanyDtos.toCompanyDto(company);
    }
}

package company;

import helpers.annotations.UserAnnotation;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.user.UserAccessMiddleware;
import models.body.UserLoginRequest;
import models.enums.UserType;
import models.repos.CollegeRepository;
import models.repos.CompanyCollegeRepository;
import models.repos.CompanyRepository;
import models.sql.College;
import models.sql.Company;
import models.sql.CompanyCollege;

import java.util.ArrayList;
import java.util.Map;

@UserAnnotation
public enum LinkCompanyCollegeController implements BaseController {

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
        if (!userType.equals(UserType.COLLEGE_ADMIN) && !userType.equals(UserType.TPO)) {
            throw new RoutingError("Only college admins and TPOs can link companies");
        }

        Long companyId = Long.parseLong(request.getRequest().get("companyId"));

        // TPO can only link to their own college
        Long collegeId = request.getUser().college.getId();

        Company company = CompanyRepository.INSTANCE.byId(companyId);
        if (company == null) {
            throw new RoutingError("Company not found");
        }

        College college = CollegeRepository.INSTANCE.byId(collegeId);
        if (college == null) {
            throw new RoutingError("College not found");
        }

        // Check if already linked
        CompanyCollege existing = CompanyCollegeRepository.INSTANCE.byCompanyAndCollege(companyId, collegeId);
        if (existing != null) {
            throw new RoutingError("Company is already linked to this college");
        }

        CompanyCollege cc = new CompanyCollege();
        cc.company = company;
        cc.college = college;
        cc.companyCanManage = request.getRequest().isPresent("companyCanManage") && Boolean.parseBoolean(request.getRequest().get("companyCanManage"));
        cc.save();

        return cc;
    }
}

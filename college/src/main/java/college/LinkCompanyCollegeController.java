package college;

import helpers.annotations.UserAnnotation;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.user.UserAccessMiddleware;
import models.body.UserLoginRequest;
import models.enums.UserType;
import models.json.CollegeDtos;
import models.repos.CollegeRepository;
import models.repos.CompanyCollegeRepository;
import models.repos.CompanyRepository;
import models.sql.College;
import models.sql.Company;
import models.sql.CompanyCollege;

import java.util.ArrayList;

/**
 * TPO links a company to their college.
 * This is a college-portal operation — only TPO/COLLEGE_ADMIN of their own college.
 */
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
        Long collegeId = request.getUser().college.getId();

        Company company = CompanyRepository.INSTANCE.byId(companyId);
        if (company == null) {
            throw new RoutingError("Company not found");
        }

        College college = CollegeRepository.INSTANCE.byId(collegeId);
        if (college == null) {
            throw new RoutingError("College not found");
        }

        CompanyCollege existing = CompanyCollegeRepository.INSTANCE.byCompanyAndCollege(companyId, collegeId);
        if (existing != null) {
            throw new RoutingError("Company is already linked to this college");
        }

        CompanyCollege cc = new CompanyCollege();
        cc.company = company;
        cc.college = college;
        cc.companyCanManage = request.getRequest().isPresent("companyCanManage") && Boolean.parseBoolean(request.getRequest().get("companyCanManage"));
        cc.save();

        return CollegeDtos.toCompanyCollegeDto(cc);
    }
}

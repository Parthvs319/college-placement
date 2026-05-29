package college;

import helpers.annotations.CollegeRole;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.college.CollegeAccessMiddleware;
import models.body.CollegeLoginRequest;
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
@CollegeRole
public enum LinkCompanyCollegeController implements BaseController {

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
        Long companyId = Long.parseLong(request.getRequest().get("companyId"));
        Long collegeId = request.getCollege().getId();

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

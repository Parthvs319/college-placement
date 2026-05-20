package student;

import helpers.annotations.UserAnnotation;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.Request;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.user.UserAccessMiddleware;
import models.body.UserLoginRequest;
import models.enums.RoundType;
import models.repos.CompanyRepository;
import models.repos.StudentRepository;
import models.sql.Company;
import models.sql.PYQ;
import models.sql.Student;

import java.util.ArrayList;

@UserAnnotation
public enum ContributePYQController implements BaseController {

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
        Student student = StudentRepository.INSTANCE.byUserId(request.getUser().getId());
        if (student == null) {
            throw new RoutingError("Student profile not found");
        }

        Request body = request.getRequest();

        String companyIdStr = body.get("companyId");
        String content = body.get("content");
        String roundTypeStr = body.get("roundType");

        if (companyIdStr == null || content == null || roundTypeStr == null) {
            throw new RoutingError("companyId, content, and roundType are required");
        }

        Company company = CompanyRepository.INSTANCE.byId(Long.parseLong(companyIdStr));
        if (company == null) {
            throw new RoutingError("Company not found");
        }

        PYQ pyq = new PYQ();
        pyq.company = company;
        pyq.college = student.college;
        pyq.content = content;
        pyq.roundType = RoundType.valueOf(roundTypeStr);
        if (body.isPresent("role")) pyq.role = body.get("role");
        if (body.isPresent("year")) pyq.year = Integer.parseInt(body.get("year"));
        if (body.isPresent("difficulty")) pyq.difficulty = body.get("difficulty");
        boolean anonymous = !body.isPresent("anonymous") || Boolean.parseBoolean(body.get("anonymous"));
        pyq.anonymous = anonymous;
        if (!anonymous) {
            pyq.contributedByStudent = student;
        }

        pyq.save();
        return pyq;
    }
}

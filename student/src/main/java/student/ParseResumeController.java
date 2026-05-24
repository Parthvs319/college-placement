package student;

import helpers.annotations.UserAnnotation;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.user.UserAccessMiddleware;
import models.body.UserLoginRequest;
import models.services.AIService;
import models.sql.Student;

import java.util.ArrayList;

/**
 * [PREMIUM] Parses the student's uploaded resume and extracts structured profile data.
 * Student can review the extracted data and save it to their profile.
 *
 * POST /premium/parse-resume
 *
 * Response: { name, email, mobile, skills[], certifications[],
 *             linkedinUrl, githubUrl, portfolioUrl,
 *             education[], experience[], projects[], summary }
 */
@UserAnnotation
public enum ParseResumeController implements BaseController {

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
        Student student = PremiumUtils.getVerifiedPremiumStudent(request);
        String resumeText = PremiumUtils.getResumeText(student);
        return AIService.parseResumeToProfile(resumeText).getMap();
    }
}

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
 * [PREMIUM] Returns a detailed ATS (Applicant Tracking System) score
 * with breakdown by formatting, keywords, sections, quantification, readability.
 *
 * GET /premium/ats-score
 *
 * Response: { score, formatting, keywords, sections, quantification,
 *             readability, strengths[], weaknesses[], missingSections[] }
 */
@UserAnnotation
public enum AtsScoreController implements BaseController {

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
        return AIService.calculateAtsScore(resumeText).getMap();
    }
}

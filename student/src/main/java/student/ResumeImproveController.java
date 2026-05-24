package student;

import helpers.annotations.UserAnnotation;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.user.UserAccessMiddleware;
import models.body.UserLoginRequest;
import models.services.AIService;
import models.sql.Student;

import java.util.ArrayList;

/**
 * [PREMIUM] Suggests improvements to the student's resume for a target role.
 * Returns prioritized suggestions, keywords to add, project ideas, and certifications.
 *
 * POST /premium/resume-improve
 * Body: { "targetRole": "Software Engineer" }
 *
 * Response: { overallAdvice, suggestions[{section, issue, fix, priority}],
 *             keywordsToAdd[], projectIdeas[], certificationSuggestions[] }
 */
@UserAnnotation
public enum ResumeImproveController implements BaseController {

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

        String targetRole = request.getRequest().get("targetRole");
        if (targetRole == null || targetRole.isEmpty()) {
            throw new RoutingError("targetRole is required");
        }

        return AIService.suggestImprovements(resumeText, targetRole).getMap();
    }
}

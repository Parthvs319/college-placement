package student;

import helpers.annotations.UserAnnotation;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.user.UserAccessMiddleware;
import models.body.UserLoginRequest;
import models.services.AIService;
import models.sql.Student;

import java.util.ArrayList;

/**
 * [PREMIUM] Generates an ATS-optimized resume from the student's profile data.
 * Returns structured resume content ready for PDF generation on the frontend.
 *
 * POST /premium/generate-resume
 *
 * Response: { summary, education[], skills{technical[], soft[]},
 *             experience[], projects[], certifications[], achievements[],
 *             atsScore, tips[] }
 */
@UserAnnotation
public enum GenerateResumeController implements BaseController {

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

        JsonObject profileData = new JsonObject()
                .put("name", student.user != null ? student.user.name : "")
                .put("email", student.user != null ? student.user.email : "")
                .put("department", student.department)
                .put("passingYear", student.passingYear)
                .put("cgpa", student.cgpa != null ? student.cgpa.toPlainString() : "")
                .put("skills", student.skills)
                .put("certifications", student.certifications)
                .put("linkedinUrl", student.linkedinUrl)
                .put("githubUrl", student.githubUrl)
                .put("portfolioUrl", student.portfolioUrl)
                .put("tenthPercentage", student.tenthPercentage)
                .put("twelfthPercentage", student.twelfthPercentage);

        return AIService.generateAtsResume(profileData).getMap();
    }
}

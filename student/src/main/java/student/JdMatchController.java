package student;

import helpers.annotations.StudentRole;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.student.StudentAccessMiddleware;
import models.body.StudentLoginRequest;
import models.repos.DriveRepository;
import models.services.AIService;
import models.sql.Drive;
import models.sql.Student;

import java.util.ArrayList;

/**
 * [PREMIUM] Matches a student's resume against a drive's job description.
 * Returns match percentage, matched/missing skills, and gap analysis.
 *
 * GET /premium/match/:driveId
 *
 * Response: { matchPercentage, skillMatch, experienceMatch,
 *             matchedSkills[], missingSkills[], bonusSkills[],
 *             fitSummary, improvementAreas[] }
 */
@StudentRole
public enum JdMatchController implements BaseController {

    INSTANCE;

    @Override
    public void handle(RoutingContext event) {
        StudentAccessMiddleware.INSTANCE.with(event, new ArrayList<>(), this.getClass())
                .map(this::map)
                .subscribe(
                        o -> ResponseUtils.INSTANCE.writeJsonResponse(event, o),
                        error -> ResponseUtils.INSTANCE.handleError(event, error)
                );
    }

    private Object map(StudentLoginRequest request) {
        Student student = PremiumUtils.getVerifiedPremiumStudent(request);
        String resumeText = PremiumUtils.getResumeText(student);

        Long driveId = Long.parseLong(request.getRoutingContext().pathParam("driveId"));
        Drive drive = DriveRepository.INSTANCE.byId(driveId);
        if (drive == null) {
            throw new RoutingError("Drive not found");
        }

        String jdText = drive.jobDescription;
        if (jdText == null || jdText.isEmpty()) {
            throw new RoutingError("This drive has no job description to match against");
        }

        return AIService.matchJdProfile(resumeText, jdText).getMap();
    }
}

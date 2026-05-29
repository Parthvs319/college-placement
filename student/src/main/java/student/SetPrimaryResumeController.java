package student;

import helpers.annotations.StudentRole;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.student.StudentAccessMiddleware;
import models.body.StudentLoginRequest;
import models.enums.UserType;
import models.repos.ResumeRepository;
import models.repos.StudentRepository;
import models.sql.Resume;
import models.sql.Student;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Marks a specific resume as the student's primary resume.
 * The primary resume is used for placement applications and AI features.
 *
 * PUT /me/resumes/:resumeId/primary
 */
@StudentRole
public enum SetPrimaryResumeController implements BaseController {

    INSTANCE;

    @Override
    public void handle(RoutingContext event) {
        StudentAccessMiddleware.INSTANCE.with(event, new ArrayList<>(), this.getClass())
                .map(req -> map(req, event))
                .subscribe(
                        o -> ResponseUtils.INSTANCE.writeJsonResponse(event, o),
                        error -> ResponseUtils.INSTANCE.handleError(event, error)
                );
    }

    private Object map(StudentLoginRequest request, RoutingContext event) {
        if (!request.getUser().getUserType().equals(UserType.STUDENT)) {
            throw new RoutingError("Only students can manage resumes");
        }
        Student student = StudentRepository.INSTANCE.byUserId(request.getUser().getId());
        if (student == null) {
            throw new RoutingError("Student profile not found");
        }

        Long resumeId = Long.parseLong(event.pathParam("resumeId"));
        Resume resume = ResumeRepository.INSTANCE.byId(resumeId);
        if (resume == null || !resume.student.getId().equals(student.getId())) {
            throw new RoutingError("Resume not found");
        }

        // Clear existing primary, then set new one
        ResumeRepository.INSTANCE.clearPrimary(student.getId());
        resume.primary = true;
        resume.update();

        // Keep legacy field in sync
        student.resumeUrl = resume.url;
        student.atsScore = resume.atsScore;
        student.update();

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Resume set as primary");
        response.put("resumeId", resume.getId());
        response.put("fileName", resume.fileName);
        return response;
    }
}

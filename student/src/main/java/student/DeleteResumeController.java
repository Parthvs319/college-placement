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
import models.services.S3Service;
import models.sql.Resume;
import models.sql.Student;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Deletes a student's resume from S3 and the database.
 * If the deleted resume was primary, the most recent remaining resume
 * is automatically promoted to primary.
 *
 * DELETE /me/resumes/:resumeId
 */
@StudentRole
public enum DeleteResumeController implements BaseController {

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

        boolean wasPrimary = resume.primary;

        // Delete from S3
        try {
            S3Service.delete(resume.s3Key);
        } catch (Exception e) {
            System.out.println("[ResumeDelete] S3 delete failed for " + resume.s3Key + ": " + e.getMessage());
        }

        // Soft delete
        resume.setDeleted(true);
        resume.update();

        // If deleted resume was primary, promote the most recent remaining resume
        if (wasPrimary) {
            List<Resume> remaining = ResumeRepository.INSTANCE.byStudent(student.getId());
            if (!remaining.isEmpty()) {
                Resume newPrimary = remaining.get(0);
                newPrimary.primary = true;
                newPrimary.update();
                student.resumeUrl = newPrimary.url;
                student.atsScore = newPrimary.atsScore;
            } else {
                student.resumeUrl = null;
                student.atsScore = null;
            }
            student.update();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Resume deleted" + (wasPrimary ? ". Next resume promoted to primary." : ""));
        response.put("resumeId", resumeId);
        return response;
    }
}

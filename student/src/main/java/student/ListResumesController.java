package student;

import helpers.annotations.UserAnnotation;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import lombok.Data;
import models.access.middlewear.user.UserAccessMiddleware;
import models.body.UserLoginRequest;
import models.enums.UserType;
import models.repos.ResumeRepository;
import models.repos.StudentRepository;
import models.sql.Resume;
import models.sql.Student;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Lists all resumes uploaded by the student.
 * Primary resume is returned first.
 *
 * GET /me/resumes
 */
@UserAnnotation
public enum ListResumesController implements BaseController {

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
        if (!request.getUser().getUserType().equals(UserType.STUDENT)) {
            throw new RoutingError("Only students can view resumes");
        }
        Student student = StudentRepository.INSTANCE.byUserId(request.getUser().getId());
        if (student == null) {
            throw new RoutingError("Student profile not found");
        }

        List<Resume> resumes = ResumeRepository.INSTANCE.byStudent(student.getId());
        return resumes.stream().map(r -> {
            ResumeDto dto = new ResumeDto();
            dto.id = r.getId();
            dto.fileName = r.fileName;
            dto.url = r.url;
            dto.contentType = r.contentType;
            dto.fileSize = r.fileSize;
            dto.primary = r.primary;
            dto.label = r.label;
            dto.atsScore = r.atsScore;
            dto.uploadedAt = r.getCreatedAt() != null ? r.getCreatedAt().toString() : null;
            return dto;
        }).collect(Collectors.toList());
    }

    @Data
    static class ResumeDto {
        Long id;
        String fileName;
        String url;
        String contentType;
        long fileSize;
        boolean primary;
        String label;
        Integer atsScore;
        String uploadedAt;
    }
}

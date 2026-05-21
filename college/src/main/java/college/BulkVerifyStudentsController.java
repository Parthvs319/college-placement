package college;

import helpers.annotations.UserAnnotation;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import lombok.Data;
import models.access.middlewear.user.UserAccessMiddleware;
import models.body.UserLoginRequest;
import models.enums.UserType;
import models.repos.StudentRepository;
import models.sql.Student;
import models.sql.User;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * TPO / College Admin verifies multiple students at once.
 *
 * POST /students/verify-bulk
 * Body: { "studentIds": [1, 2, 3, ...] }
 */
@UserAnnotation
public enum BulkVerifyStudentsController implements BaseController {

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
        UserType userType = request.getUser().getUserType();
        if (!userType.equals(UserType.COLLEGE_ADMIN) && !userType.equals(UserType.TPO)) {
            throw new RoutingError("Only TPO and College Admins can verify students");
        }

        Long myCollegeId = request.getUser().college.getId();

        Object idsObj = request.getRequest().get("studentIds");
        if (idsObj == null || !(idsObj instanceof List)) {
            throw new RoutingError("studentIds array is required");
        }

        List<Long> studentIds = ((List<?>) idsObj).stream()
                .map(o -> Long.parseLong(o.toString()))
                .collect(Collectors.toList());

        if (studentIds.isEmpty()) {
            throw new RoutingError("studentIds cannot be empty");
        }

        List<BulkVerifyItem> results = new ArrayList<>();

        for (Long studentId : studentIds) {
            BulkVerifyItem item = new BulkVerifyItem();
            item.studentId = studentId;

            Student student = StudentRepository.INSTANCE.byId(studentId);
            if (student == null) {
                item.success = false;
                item.message = "Student not found";
                results.add(item);
                continue;
            }

            if (!student.college.getId().equals(myCollegeId)) {
                item.success = false;
                item.message = "Student belongs to a different college";
                results.add(item);
                continue;
            }

            User studentUser = student.user;
            if (studentUser.verified) {
                item.success = true;
                item.message = "Already verified";
                item.name = studentUser.name;
                item.enrollmentNumber = student.enrollmentNumber;
                results.add(item);
                continue;
            }

            studentUser.verified = true;
            studentUser.update();

            item.success = true;
            item.message = "Verified successfully";
            item.name = studentUser.name;
            item.enrollmentNumber = student.enrollmentNumber;
            results.add(item);
        }

        BulkVerifyResponse response = new BulkVerifyResponse();
        response.total = results.size();
        response.verified = (int) results.stream().filter(r -> r.success).count();
        response.failed = response.total - response.verified;
        response.results = results;
        return response;
    }

    @Data
    static class BulkVerifyItem {
        Long studentId;
        String name;
        String enrollmentNumber;
        boolean success;
        String message;
    }

    @Data
    static class BulkVerifyResponse {
        int total;
        int verified;
        int failed;
        List<BulkVerifyItem> results;
    }
}

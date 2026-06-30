package college.student;

import helpers.annotations.CollegeRole;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import lombok.Data;
import models.access.middlewear.college.CollegeAccessMiddleware;
import models.body.CollegeLoginRequest;
import models.repos.StudentRepository;
import models.sql.Student;
import models.sql.User;

import java.util.ArrayList;
import java.util.List;

/**
 * POST /college/students/verify-by-enrollment
 *
 * Accepts a list of enrollment numbers and verifies matching students.
 * TPO can paste enrollment numbers from Excel or comma/newline separated.
 *
 * Body (JSON):
 * {
 *   "enrollmentNumbers": ["ENR001", "ENR002", "ENR003"]
 * }
 *
 * Response:
 * {
 *   "total": 3,
 *   "verified": 2,
 *   "alreadyVerified": 0,
 *   "notFound": 1,
 *   "results": [
 *     { "enrollmentNumber": "ENR001", "status": "VERIFIED", "name": "Alice", "studentId": 1 },
 *     { "enrollmentNumber": "ENR002", "status": "ALREADY_VERIFIED", "name": "Bob", "studentId": 2 },
 *     { "enrollmentNumber": "ENR003", "status": "NOT_FOUND", "name": null, "studentId": null }
 *   ]
 * }
 */
@CollegeRole
public enum BulkVerifyByEnrollmentController implements BaseController {

    INSTANCE;

    @Override
    public void handle(RoutingContext event) {
        CollegeAccessMiddleware.INSTANCE.with(event, new ArrayList<>(), this.getClass())
                .map(this::map)
                .subscribe(
                        o -> ResponseUtils.INSTANCE.writeJsonResponse(event, o),
                        error -> ResponseUtils.INSTANCE.handleError(event, error)
                );
    }

    private Object map(CollegeLoginRequest request) {
        Long myCollegeId = request.getCollege().getId();

        Object rawList = request.getRequest().get("enrollmentNumbers");
        if (rawList == null || !(rawList instanceof List)) {
            throw new RoutingError("enrollmentNumbers array is required");
        }

        List<String> enrollmentNumbers = new ArrayList<>();
        for (Object item : (List<?>) rawList) {
            String val = String.valueOf(item).trim();
            if (!val.isBlank()) enrollmentNumbers.add(val);
        }

        if (enrollmentNumbers.isEmpty()) {
            throw new RoutingError("At least one enrollment number is required");
        }

        List<ResultItem> results = new ArrayList<>();
        int verifiedCount = 0;
        int alreadyCount  = 0;
        int notFoundCount = 0;

        for (String enrollment : enrollmentNumbers) {
            ResultItem item = new ResultItem();
            item.enrollmentNumber = enrollment;

            Student student = StudentRepository.INSTANCE.byEnrollment(enrollment, myCollegeId);

            if (student == null) {
                item.status = "NOT_FOUND";
                notFoundCount++;
                results.add(item);
                continue;
            }

            item.studentId = student.getId();
            User studentUser = student.getUser();
            item.name = studentUser != null ? studentUser.getName() : null;
            item.email = studentUser != null ? studentUser.getEmail() : null;

            if (studentUser != null && studentUser.isVerified()) {
                item.status = "ALREADY_VERIFIED";
                alreadyCount++;
                results.add(item);
                continue;
            }

            if (studentUser != null) {
                studentUser.verified = true;
                studentUser.update();
                item.status = "VERIFIED";
                verifiedCount++;
            } else {
                item.status = "NOT_FOUND";
                notFoundCount++;
            }

            results.add(item);
        }

        BulkVerifyResponse response = new BulkVerifyResponse();
        response.total = enrollmentNumbers.size();
        response.verified = verifiedCount;
        response.alreadyVerified = alreadyCount;
        response.notFound = notFoundCount;
        response.results = results;
        return response;
    }

    @Data
    static class ResultItem {
        String enrollmentNumber;
        String status;
        String name;
        String email;
        Long studentId;
    }

    @Data
    static class BulkVerifyResponse {
        int total;
        int verified;
        int alreadyVerified;
        int notFound;
        List<ResultItem> results;
    }
}

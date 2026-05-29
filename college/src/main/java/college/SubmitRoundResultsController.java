package college;

import helpers.annotations.CollegeRole;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.Request;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.college.CollegeAccessMiddleware;
import models.body.CollegeLoginRequest;
import models.enums.ApplicationStatus;
import models.enums.Status;
import models.json.CollegeDtos;
import models.repos.DriveApplicationRepository;
import models.repos.DriveRoundRepository;
import models.repos.RoundResultRepository;
import models.repos.StudentRepository;
import models.sql.DriveApplication;
import models.sql.DriveRound;
import models.sql.RoundResult;
import models.sql.Student;

import java.math.BigDecimal;
import java.util.ArrayList;

/**
 * Submit result for a single student in a round.
 * Body: { studentId, status (APPROVED/REJECTED), score, feedback, interviewerName }
 */
@CollegeRole
public enum SubmitRoundResultsController implements BaseController {

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
        String roundIdParam = request.getRoutingContext().pathParam("roundId");
        DriveRound round = DriveRoundRepository.INSTANCE.byId(Long.parseLong(roundIdParam));
        if (round == null) {
            throw new RoutingError("Round not found");
        }

        Request body = request.getRequest();
        String studentIdStr = body.get("studentId");
        String statusStr = body.get("status");

        if (studentIdStr == null || statusStr == null) {
            throw new RoutingError("studentId and status are required");
        }

        Long studentId = Long.parseLong(studentIdStr);
        Student student = StudentRepository.INSTANCE.byId(studentId);
        if (student == null) {
            throw new RoutingError("Student not found");
        }

        RoundResult existing = RoundResultRepository.INSTANCE.byRoundAndStudent(round.getId(), studentId);
        if (existing != null) {
            existing.status = Status.valueOf(statusStr);
            if (body.isPresent("score")) existing.score = new BigDecimal(String.valueOf(body.get("score")));
            if (body.isPresent("feedback")) existing.feedback = body.get("feedback");
            if (body.isPresent("interviewerName")) existing.interviewerName = body.get("interviewerName");
            existing.update();

            updateApplicationStatus(round, studentId, Status.valueOf(statusStr));
            return CollegeDtos.toRoundResultDto(existing);
        }

        RoundResult result = new RoundResult();
        result.round = round;
        result.student = student;
        result.status = Status.valueOf(statusStr);

        if (body.isPresent("score")) result.score = new BigDecimal(String.valueOf(body.get("score")));
        if (body.isPresent("feedback")) result.feedback = body.get("feedback");
        if (body.isPresent("interviewerName")) result.interviewerName = body.get("interviewerName");

        result.save();

        updateApplicationStatus(round, studentId, Status.valueOf(statusStr));
        return CollegeDtos.toRoundResultDto(result);
    }

    private void updateApplicationStatus(DriveRound round, Long studentId, Status roundStatus) {
        DriveApplication app = DriveApplicationRepository.INSTANCE
                .byStudentAndDrive(studentId, round.drive.getId());
        if (app != null) {
            if (roundStatus == Status.REJECTED) {
                app.status = ApplicationStatus.REJECTED;
            } else if (roundStatus == Status.APPROVED) {
                app.status = ApplicationStatus.IN_PROCESS;
            }
            app.update();
        }
    }
}

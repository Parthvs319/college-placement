package student;

import helpers.annotations.StudentRole;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.Request;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.student.StudentAccessMiddleware;
import models.body.StudentLoginRequest;
import models.enums.ApplicationStatus;
import models.enums.DriveStatus;
import models.repos.*;
import models.sql.*;

import java.util.ArrayList;
import java.util.List;


@StudentRole
public enum ApplyToDriveController implements BaseController {

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
        Student student = StudentRepository.INSTANCE.byUserId(request.getUser().getId());
        if (student == null) {
            throw new RoutingError("Student profile not found. Complete onboarding first.");
        }
        // Must be verified by TPO or email domain match
        if (!request.getUser().verified) {
            throw new RoutingError("Your account is not yet verified. Please wait for TPO approval.");
        }
        String driveIdParam = request.getRoutingContext().pathParam("driveId");
        Drive drive = DriveRepository.INSTANCE.byId(Long.parseLong(driveIdParam));
        if (drive == null) {
            throw new RoutingError("Drive not found");
        }
        if (!drive.status.equals(DriveStatus.REGISTRATION_OPEN)) {
            throw new RoutingError("Drive is not open for registration. Current status: " + drive.status);
        }
        DriveApplication existing = DriveApplicationRepository.INSTANCE.byStudentAndDrive(student.getId(), drive.getId());
        if (existing != null) {
            throw new RoutingError("You have already applied to this drive");
        }
        if (student.optedOut) {
            throw new RoutingError("You have opted out of placements");
        }
        PlacementPolicy policy = PlacementPolicyRepository.INSTANCE.latestByCollege(student.college.getId());
        if (policy != null && policy.blockAfterFirstAccept) {
            List<Offer> accepted = OfferRepository.INSTANCE.acceptedByStudent(student.getId());
            if (!accepted.isEmpty()) {
                throw new RoutingError("Policy blocks further applications after accepting an offer");
            }
        }
        if (drive.minCgpa != null && student.cgpa != null && student.cgpa.compareTo(drive.minCgpa) < 0) {
            throw new RoutingError("You do not meet the minimum CGPA requirement (" + drive.minCgpa + ")");
        }
        if (student.activeBacklogs > drive.maxActiveBacklogs) {
            throw new RoutingError("You exceed the maximum allowed active backlogs (" + drive.maxActiveBacklogs + ")");
        }
        if (drive.eligibleDepartments != null && !drive.eligibleDepartments.isEmpty()) {
            if (!drive.eligibleDepartments.contains(student.department)) {
                throw new RoutingError("Your department (" + student.department + ") is not eligible for this drive");
            }
        }
        if (drive.minPassingYear != null && student.passingYear < drive.minPassingYear) {
            throw new RoutingError("Your passing year does not meet the requirement");
        }
        if (drive.maxPassingYear != null && student.passingYear > drive.maxPassingYear) {
            throw new RoutingError("Your passing year does not meet the requirement");
        }
        DriveApplication application = new DriveApplication();
        application.student = student;
        application.drive = drive;
        application.status = ApplicationStatus.APPLIED;
        application.resumeSnapshot = student.resumeUrl;
        Request body = request.getRequest();
        if (body.isPresent("notes")) {
            application.notes = body.get("notes");
        }
        application.save();
        return StudentDtos.toApplicationDto(application);
    }
}

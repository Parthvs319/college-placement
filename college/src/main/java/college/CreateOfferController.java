package college;

import helpers.annotations.UserAnnotation;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.Request;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.user.UserAccessMiddleware;
import models.body.UserLoginRequest;
import models.enums.ApplicationStatus;
import models.enums.OfferStatus;
import models.enums.UserType;
import models.repos.*;
import models.sql.*;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;

@UserAnnotation
public enum CreateOfferController implements BaseController {

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
            throw new RoutingError("Not authorized to create offers");
        }
        Request body = request.getRequest();
        String driveIdParam = request.getRoutingContext().pathParam("driveId");
        Long driveId = Long.parseLong(driveIdParam);
        Drive drive = DriveRepository.INSTANCE.byId(driveId);
        if (drive == null) {
            throw new RoutingError("Drive not found");
        }
        String studentIdStr = body.get("studentId");
        String ctcStr = body.get("ctcOffered");
        if (studentIdStr == null || ctcStr == null) {
            throw new RoutingError("studentId and ctcOffered are required");
        }
        Long studentId = Long.parseLong(studentIdStr);
        Student student = StudentRepository.INSTANCE.byId(studentId);
        if (student == null) {
            throw new RoutingError("Student not found");
        }
        Offer existing = OfferRepository.INSTANCE.byStudentAndDrive(studentId, driveId);
        if (existing != null) {
            throw new RoutingError("Offer already exists for this student in this drive");
        }
        Offer offer = new Offer();
        offer.student = student;
        offer.drive = drive;
        offer.ctcOffered = new BigDecimal(ctcStr);
        offer.status = OfferStatus.PENDING;
        if (body.isPresent("designation")) offer.designation = body.get("designation");
        if (body.isPresent("location")) offer.location = body.get("location");
        if (body.isPresent("offerLetterUrl")) offer.offerLetterUrl = body.get("offerLetterUrl");
        if (body.isPresent("notes")) offer.notes = body.get("notes");
        if (body.isPresent("responseDeadline")) {
            offer.responseDeadline = Timestamp.valueOf(String.valueOf(body.get("responseDeadline")));
        } else {
            PlacementPolicy policy = PlacementPolicyRepository.INSTANCE.latestByCollege(student.college.getId());
            if (policy != null) {
                long expiryMillis = (long) policy.offerExpiryDays * 24 * 60 * 60 * 1000;
                offer.responseDeadline = new Timestamp(System.currentTimeMillis() + expiryMillis);
            }
        }
        offer.save();
        DriveApplication app = DriveApplicationRepository.INSTANCE.byStudentAndDrive(studentId, driveId);
        if (app != null) {
            app.status = ApplicationStatus.SELECTED;
            app.update();
        }
        return offer;
    }
}

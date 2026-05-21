package college;

import helpers.annotations.UserAnnotation;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.Request;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.user.UserAccessMiddleware;
import models.body.UserLoginRequest;
import models.enums.DriveStatus;
import models.enums.EmploymentType;
import models.enums.UserType;
import models.json.CollegeDtos;
import models.repos.CompanyCollegeRepository;
import models.sql.CompanyCollege;
import models.sql.Drive;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;

@UserAnnotation
public enum CreateDriveController implements BaseController {

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
            throw new RoutingError("Not authorized to create drives");
        }
        Request body = request.getRequest();
        String companyCollegeIdStr = body.get("companyCollegeId");
        String title = body.get("title");
        String employmentTypeStr = body.get("employmentType");
        if (companyCollegeIdStr == null || title == null || employmentTypeStr == null) {
            throw new RoutingError("companyCollegeId, title, and employmentType are required");
        }
        CompanyCollege cc = CompanyCollegeRepository.INSTANCE.byId(Long.parseLong(companyCollegeIdStr));
        if (cc == null) {
            throw new RoutingError("Company-College link not found");
        }

        // Verify TPO is creating drive for their own college
        if (!cc.college.getId().equals(request.getUser().college.getId())) {
            throw new RoutingError("You can only create drives for your own college");
        }

        Drive drive = new Drive();
        drive.companyCollege = cc;
        drive.title = title;
        drive.employmentType = EmploymentType.valueOf(employmentTypeStr);
        drive.status = DriveStatus.UPCOMING;

        if (body.isPresent("jobDescription")) drive.jobDescription = body.get("jobDescription");
        if (body.isPresent("academicYear")) drive.academicYear = Integer.parseInt(body.get("academicYear"));
        if (body.isPresent("minCgpa")) drive.minCgpa = new BigDecimal(String.valueOf(body.get("minCgpa")));
        if (body.isPresent("maxActiveBacklogs")) drive.maxActiveBacklogs = Integer.parseInt(body.get("maxActiveBacklogs"));
        if (body.isPresent("ctcOffered")) drive.ctcOffered = new BigDecimal(String.valueOf(body.get("ctcOffered")));
        if (body.isPresent("stipend")) drive.stipend = new BigDecimal(String.valueOf(body.get("stipend")));
        if (body.isPresent("location")) drive.location = body.get("location");
        if (body.isPresent("isRemote")) drive.isRemote = Boolean.parseBoolean(body.get("isRemote"));
        if (body.isPresent("venue")) drive.venue = body.get("venue");
        if (body.isPresent("registrationDeadline")) {
            drive.registrationDeadline = Timestamp.valueOf(String.valueOf(body.get("registrationDeadline")));
        }
        if (body.isPresent("driveDate")) {
            drive.driveDate = Timestamp.valueOf(String.valueOf(body.get("driveDate")));
        }
        if (body.isPresent("minPassingYear")) drive.minPassingYear = Integer.parseInt(body.get("minPassingYear"));
        if (body.isPresent("maxPassingYear")) drive.maxPassingYear = Integer.parseInt(body.get("maxPassingYear"));

        drive.save();
        return CollegeDtos.toDriveDto(drive);
    }
}

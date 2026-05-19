package college;

import helpers.annotations.UserAnnotation;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.user.UserAccessMiddleware;
import models.body.UserLoginRequest;
import models.enums.UserType;
import models.repos.CollegeRepository;
import models.sql.College;
import models.sql.Document;

import java.util.ArrayList;
import java.util.Map;

@UserAnnotation
public enum UploadDocumentController implements BaseController {

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
            throw new RoutingError("Only college admins and TPOs can upload documents");
        }

        String collegeIdParam = request.getRoutingContext().pathParam("collegeId");
        Long collegeId = Long.parseLong(collegeIdParam);
        College college = CollegeRepository.INSTANCE.byId(collegeId);
        if (college == null) {
            throw new RoutingError("College not found");
        }
        Document doc = new Document();
        doc.college = college;
        doc.name = request.getRequest().get("name");
        doc.type = request.getRequest().get("type");
        doc.fileUrl = request.getRequest().get("fileUrl");
        doc.fileType = request.getRequest().get("fileType");
        doc.uploadedByUser = request.getUser();

        if (request.getRequest().isPresent("academicYear")) {
            doc.academicYear = Integer.parseInt(request.getRequest().get("academicYear"));
        }
        if (request.getRequest().isPresent("fileSizeBytes")) {
            doc.fileSizeBytes = Long.parseLong(request.getRequest().get("fileSizeBytes"));
        }

        doc.save();
        return doc;
    }
}

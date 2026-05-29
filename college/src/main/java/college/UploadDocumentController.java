package college;

import helpers.annotations.CollegeRole;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.college.CollegeAccessMiddleware;
import models.body.CollegeLoginRequest;
import models.json.CollegeDtos;
import models.repos.CollegeRepository;
import models.sql.College;
import models.sql.Document;

import java.util.ArrayList;

@CollegeRole
public enum UploadDocumentController implements BaseController {

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
        Long collegeId = request.getCollege().getId();
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
        return CollegeDtos.toDocumentDto(doc);
    }
}

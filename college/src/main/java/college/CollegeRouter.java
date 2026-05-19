package college;

import helpers.interfaces.SubRouterProtocol;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.web.Router;

public enum CollegeRouter implements SubRouterProtocol {

    INSTANCE;

    @Override
    public Router router(Vertx vertx) {
        Router router = Router.router(vertx);

        // College CRUD
        router.post("/create").handler(CreateCollegeController.INSTANCE::handle);
        router.get("/:id").handler(GetCollegeController.INSTANCE::handle);
        router.put("/:id").handler(UpdateCollegeController.INSTANCE::handle);
        router.get("/list").handler(ListCollegesController.INSTANCE::handle);

        router.post("/:collegeId/policy").handler(CreatePolicyController.INSTANCE::handle);
        router.get("/:collegeId/policy").handler(GetPolicyController.INSTANCE::handle);

        // Documents
        router.post("/:collegeId/documents").handler(UploadDocumentController.INSTANCE::handle);
        router.get("/:collegeId/documents").handler(ListDocumentsController.INSTANCE::handle);

        // Analytics
        router.get("/:collegeId/analytics").handler(CollegeAnalyticsController.INSTANCE::handle);

        return router;
    }
}

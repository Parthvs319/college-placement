package college.student;

import helpers.interfaces.SubRouterProtocol;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.web.Router;

public enum StudentSubRouter implements SubRouterProtocol {

    INSTANCE;

    @Override
    public Router router(Vertx vertx) {
        Router router = Router.router(vertx);

        // Static paths MUST be declared before parameterised :studentId routes
        router.get("/").handler(ListStudentsController.INSTANCE::handle);
        router.get("/placed").handler(ListPlacedStudentsController.INSTANCE::handle);
        router.get("/unplaced").handler(ListUnplacedStudentsController.INSTANCE::handle);
        router.get("/unverified").handler(ListUnverifiedStudentsController.INSTANCE::handle);
        router.post("/invite").handler(InviteStudentsController.INSTANCE::handle);
        router.post("/bulk-upload").handler(BulkUploadStudentsController.INSTANCE::handle);
        router.post("/onboarding-complete").handler(OnboardingCompleteController.INSTANCE::handle);
        router.post("/verify-bulk").handler(BulkVerifyStudentsController.INSTANCE::handle);
        router.post("/warn").handler(WarnStudentsController.INSTANCE::handle);

        // Parameterised student routes — must come after all static paths
        router.get("/:studentId").handler(GetStudentDetailController.INSTANCE::handle);
        router.post("/:studentId/verify").handler(VerifyStudentController.INSTANCE::handle);
        router.get("/:studentId/documents").handler(GetStudentDocumentsController.INSTANCE::handle);
        router.post("/:studentId/documents/:docId/verify").handler(VerifyStudentDocumentController.INSTANCE::handle);
        router.post("/:studentId/documents/:docId/reject").handler(RejectStudentDocumentController.INSTANCE::handle);

        return router;
    }
}

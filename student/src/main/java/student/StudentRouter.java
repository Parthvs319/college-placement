package student;

import helpers.interfaces.SubRouterProtocol;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.web.Router;

public enum StudentRouter implements SubRouterProtocol {

    INSTANCE;

    @Override
    public Router router(Vertx vertx) {
        Router router = Router.router(vertx);

        // Student profile
        router.get("/me").handler(GetMyProfileController.INSTANCE::handle);
        router.put("/me").handler(UpdateProfileController.INSTANCE::handle);
        router.get("/:id").handler(GetStudentController.INSTANCE::handle);

        // College students listing (for TPO/admin)
        router.get("/college/:collegeId").handler(ListStudentsController.INSTANCE::handle);
        router.get("/college/:collegeId/placed").handler(ListPlacedStudentsController.INSTANCE::handle);
        router.get("/college/:collegeId/unplaced").handler(ListUnplacedStudentsController.INSTANCE::handle);

        // Student's own applications & offers
        router.get("/me/applications").handler(MyApplicationsController.INSTANCE::handle);
        router.get("/me/offers").handler(MyOffersController.INSTANCE::handle);
        router.post("/me/offers/:offerId/respond").handler(RespondToOfferController.INSTANCE::handle);

        // PYQ
        router.get("/pyq/company/:companyId").handler(GetPYQController.INSTANCE::handle);
        router.post("/pyq/contribute").handler(ContributePYQController.INSTANCE::handle);

        return router;
    }
}

package student;

import helpers.interfaces.SubRouterProtocol;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.web.Router;

public enum StudentRouter implements SubRouterProtocol {

    INSTANCE;

    @Override
    public Router router(Vertx vertx) {
        Router router = Router.router(vertx);

        router.post("/onboard").handler(StudentOnboardController.INSTANCE::handle);

        router.get("/me").handler(GetMyProfileController.INSTANCE::handle);
        router.put("/me").handler(UpdateProfileController.INSTANCE::handle);

        router.get("/me/applications").handler(MyApplicationsController.INSTANCE::handle);
        router.get("/me/offers").handler(MyOffersController.INSTANCE::handle);
        router.post("/me/offers/:offerId/respond").handler(RespondToOfferController.INSTANCE::handle);

        router.get("/drives").handler(ListAvailableDrivesController.INSTANCE::handle);
        router.get("/drives/:driveId").handler(GetDriveDetailController.INSTANCE::handle);
        router.post("/drives/:driveId/apply").handler(ApplyToDriveController.INSTANCE::handle);

        router.get("/pyq/company/:companyId").handler(GetPYQController.INSTANCE::handle);
        router.post("/pyq/contribute").handler(ContributePYQController.INSTANCE::handle);

        return router;
    }
}

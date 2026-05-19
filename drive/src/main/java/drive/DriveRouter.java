package drive;

import helpers.interfaces.SubRouterProtocol;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.web.Router;

public enum DriveRouter implements SubRouterProtocol {

    INSTANCE;

    @Override
    public Router router(Vertx vertx) {
        Router router = Router.router(vertx);

        // Drive CRUD
        router.post("/create").handler(CreateDriveController.INSTANCE::handle);
        router.get("/:id").handler(GetDriveController.INSTANCE::handle);
        router.put("/:id").handler(UpdateDriveController.INSTANCE::handle);
        router.get("/college/:collegeId").handler(ListDrivesController.INSTANCE::handle);
        router.get("/college/:collegeId/upcoming").handler(ListUpcomingDrivesController.INSTANCE::handle);

        // Applications
        router.post("/:driveId/apply").handler(ApplyToDriveController.INSTANCE::handle);
        router.get("/:driveId/applications").handler(ListDriveApplicationsController.INSTANCE::handle);

        // Rounds
        router.post("/:driveId/rounds").handler(CreateRoundController.INSTANCE::handle);
        router.get("/:driveId/rounds").handler(ListRoundsController.INSTANCE::handle);
        router.post("/rounds/:roundId/results").handler(SubmitRoundResultsController.INSTANCE::handle);

        // Offers
        router.post("/:driveId/offers").handler(CreateOfferController.INSTANCE::handle);
        router.get("/:driveId/offers").handler(ListDriveOffersController.INSTANCE::handle);

        return router;
    }
}

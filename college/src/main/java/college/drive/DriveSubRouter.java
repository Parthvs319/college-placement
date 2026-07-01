package college.drive;

import helpers.interfaces.SubRouterProtocol;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.web.Router;

public enum DriveSubRouter implements SubRouterProtocol {

    INSTANCE;

    @Override
    public Router router(Vertx vertx) {
        Router router = Router.router(vertx);

        // Static drive paths before parameterised :driveId routes
        router.post("/").handler(CreateDriveController.INSTANCE::handle);
        router.get("/").handler(ListDrivesController.INSTANCE::handle);
        router.get("/upcoming").handler(ListUpcomingDrivesController.INSTANCE::handle);

        // Parameterised drive routes
        router.get("/:driveId").handler(GetDriveController.INSTANCE::handle);
        router.put("/:driveId").handler(UpdateDriveController.INSTANCE::handle);
        router.get("/:driveId/applications").handler(ListDriveApplicationsController.INSTANCE::handle);
        router.post("/:driveId/rounds").handler(CreateRoundController.INSTANCE::handle);
        router.get("/:driveId/rounds").handler(ListRoundsController.INSTANCE::handle);
        router.post("/:driveId/offers").handler(CreateOfferController.INSTANCE::handle);
        router.get("/:driveId/offers").handler(ListDriveOffersController.INSTANCE::handle);
        router.post("/:driveId/finalize-venue").handler(FinalizeVenueController.INSTANCE::handle);
        router.post("/:driveId/remind-non-applicants").handler(RemindNonApplicantsController.INSTANCE::handle);

        return router;
    }
}

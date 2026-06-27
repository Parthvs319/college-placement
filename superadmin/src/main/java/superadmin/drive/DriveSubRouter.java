package superadmin.drive;

import helpers.interfaces.SubRouterProtocol;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.web.Router;

public enum DriveSubRouter implements SubRouterProtocol {

    INSTANCE;

    @Override
    public Router router(Vertx vertx) {
        Router router = Router.router(vertx);

        router.get("/").handler(ListAllDrivesController.INSTANCE::handle);
        router.get("/:driveId").handler(GetDriveDetailController.INSTANCE::handle);

        return router;
    }
}

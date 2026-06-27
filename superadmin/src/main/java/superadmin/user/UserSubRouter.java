package superadmin.user;

import helpers.interfaces.SubRouterProtocol;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.web.Router;

public enum UserSubRouter implements SubRouterProtocol {

    INSTANCE;

    @Override
    public Router router(Vertx vertx) {
        Router router = Router.router(vertx);

        router.get("/").handler(ListAllUsersController.INSTANCE::handle);
        router.post("/:userId/toggle-active").handler(ToggleUserActiveController.INSTANCE::handle);

        return router;
    }
}

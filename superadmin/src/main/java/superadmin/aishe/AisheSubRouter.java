package superadmin.aishe;

import helpers.interfaces.SubRouterProtocol;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.web.Router;

public enum AisheSubRouter implements SubRouterProtocol {

    INSTANCE;

    @Override
    public Router router(Vertx vertx) {
        Router router = Router.router(vertx);

        router.get("/all").handler(AisheAllController.INSTANCE::handle);
        router.get("/search").handler(AisheSearchController.INSTANCE::handle);

        return router;
    }
}

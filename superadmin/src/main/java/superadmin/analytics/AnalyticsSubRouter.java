package superadmin.analytics;

import helpers.interfaces.SubRouterProtocol;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.web.Router;

public enum AnalyticsSubRouter implements SubRouterProtocol {

    INSTANCE;

    @Override
    public Router router(Vertx vertx) {
        Router router = Router.router(vertx);

        router.get("/analytics").handler(PlatformAnalyticsController.INSTANCE::handle);
        router.get("/activity").handler(PlatformActivityController.INSTANCE::handle);

        return router;
    }
}

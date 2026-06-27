package college.notification;

import helpers.interfaces.SubRouterProtocol;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.web.Router;

public enum NotificationSubRouter implements SubRouterProtocol {

    INSTANCE;

    @Override
    public Router router(Vertx vertx) {
        Router router = Router.router(vertx);

        router.post("/send").handler(SendNotificationController.INSTANCE::handle);
        router.get("/").handler(ListNotificationsController.INSTANCE::handle);

        return router;
    }
}

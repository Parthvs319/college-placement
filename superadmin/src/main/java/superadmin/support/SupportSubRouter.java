package superadmin.support;

import helpers.interfaces.SubRouterProtocol;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.web.Router;

public enum SupportSubRouter implements SubRouterProtocol {

    INSTANCE;

    @Override
    public Router router(Vertx vertx) {
        Router router = Router.router(vertx);

        router.get("/tickets").handler(ListSupportTicketsController.INSTANCE::handle);
        router.put("/tickets/:ticketId").handler(UpdateSupportTicketController.INSTANCE::handle);

        return router;
    }
}

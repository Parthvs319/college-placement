package superadmin.email;

import helpers.interfaces.SubRouterProtocol;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.web.Router;

public enum EmailSubRouter implements SubRouterProtocol {

    INSTANCE;

    @Override
    public Router router(Vertx vertx) {
        Router router = Router.router(vertx);

        router.post("/send-admin-mail").handler(SendAdminMailController.INSTANCE::handle);
        router.post("/send-promo-email").handler(SendPromoEmailController.INSTANCE::handle);
        router.post("/send-otp").handler(SendOtpController.INSTANCE::handle);
        router.post("/verify-otp").handler(VerifyOtpController.INSTANCE::handle);
        router.post("/send-action-otp").handler(SendActionOtpController.INSTANCE::handle);

        return router;
    }
}

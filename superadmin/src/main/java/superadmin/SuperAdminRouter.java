package superadmin;

import helpers.interfaces.SubRouterProtocol;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.web.Router;

public enum SuperAdminRouter implements SubRouterProtocol {

    INSTANCE;

    @Override
    public Router router(Vertx vertx) {
        System.out.println("Here4");
        Router router = Router.router(vertx);

        router.get("/analytics").handler(PlatformAnalyticsController.INSTANCE::handle);

        router.get("/colleges").handler(ListAllCollegesController.INSTANCE::handle);
        router.post("/colleges").handler(CreateCollegeController.INSTANCE::handle);
        router.get("/colleges/:collegeId").handler(GetCollegeDetailController.INSTANCE::handle);
        router.post("/colleges/:collegeId/verify").handler(VerifyCollegeController.INSTANCE::handle);
        router.post("/colleges/:collegeId/toggle-active").handler(ToggleCollegeActiveController.INSTANCE::handle);

        router.get("/students").handler(ListAllStudentsController.INSTANCE::handle);

        router.get("/companies").handler(ListAllCompaniesController.INSTANCE::handle);

        router.get("/drives").handler(ListAllDrivesController.INSTANCE::handle);

        router.get("/applications").handler(ListAllApplicationsController.INSTANCE::handle);

        router.get("/offers").handler(ListAllOffersController.INSTANCE::handle);

        router.get("/users").handler(ListAllUsersController.INSTANCE::handle);
        router.post("/users/:userId/toggle-active").handler(ToggleUserActiveController.INSTANCE::handle);

        router.get("/subscriptions").handler(ListAllSubscriptionsController.INSTANCE::handle);

        return router;
    }
}

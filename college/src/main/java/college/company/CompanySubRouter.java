package college.company;

import helpers.interfaces.SubRouterProtocol;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.web.Router;

public enum CompanySubRouter implements SubRouterProtocol {

    INSTANCE;

    @Override
    public Router router(Vertx vertx) {
        Router router = Router.router(vertx);

        router.post("/onboard").handler(OnboardCompanyController.INSTANCE::handle);
        router.post("/link").handler(LinkCompanyCollegeController.INSTANCE::handle);
        router.get("/").handler(ListCompanyCollegesController.INSTANCE::handle);
        router.post("/remind-inactive").handler(RemindInactiveCompaniesController.INSTANCE::handle);
        router.get("/:companyCollegeId/offers").handler(GetCompanyOffersController.INSTANCE::handle);

        return router;
    }
}

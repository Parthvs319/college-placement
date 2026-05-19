package company;

import helpers.interfaces.SubRouterProtocol;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.web.Router;

public enum CompanyRouter implements SubRouterProtocol {

    INSTANCE;

    @Override
    public Router router(Vertx vertx) {
        Router router = Router.router(vertx);

        router.post("/create").handler(CreateCompanyController.INSTANCE::handle);
        router.get("/:id").handler(GetCompanyController.INSTANCE::handle);
        router.get("/list").handler(ListCompaniesController.INSTANCE::handle);

        // Company-College mapping (TPO links a company to their college)
        router.post("/link").handler(LinkCompanyCollegeController.INSTANCE::handle);
        router.get("/college/:collegeId").handler(ListCompanyCollegesController.INSTANCE::handle);

        return router;
    }
}

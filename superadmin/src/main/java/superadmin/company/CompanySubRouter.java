package superadmin.company;

import helpers.interfaces.SubRouterProtocol;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.web.Router;

public enum CompanySubRouter implements SubRouterProtocol {

    INSTANCE;

    @Override
    public Router router(Vertx vertx) {
        Router router = Router.router(vertx);

        router.get("/").handler(ListAllCompaniesController.INSTANCE::handle);
        router.get("/:companyId").handler(GetCompanyDetailController.INSTANCE::handle);
        router.post("/:companyId/approve").handler(ApproveCompanyController.INSTANCE::handle);

        return router;
    }
}

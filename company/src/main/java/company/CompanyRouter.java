package company;

import helpers.interfaces.SubRouterProtocol;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.web.Router;

public enum CompanyRouter implements SubRouterProtocol {

    INSTANCE;

    @Override
    public Router router(Vertx vertx) {
        Router router = Router.router(vertx);

        // ── Company Profile CRUD ──
        router.post("/create").handler(CreateCompanyController.INSTANCE::handle);
        router.get("/list").handler(ListCompaniesController.INSTANCE::handle);
        router.get("/:id").handler(GetCompanyController.INSTANCE::handle);

        // ── Company Portal (HR view) ──
        router.get("/:companyId/colleges").handler(ListLinkedCollegesController.INSTANCE::handle);
        router.get("/:companyId/drives").handler(ListCompanyDrivesController.INSTANCE::handle);

        // ── Reports (CSV downloads) ──
        router.get("/:companyId/reports/drives").handler(CompanyDriveReportController.INSTANCE::handle);
        router.get("/:companyId/reports/drives/:driveId/applications").handler(CompanyApplicationReportController.INSTANCE::handle);

        return router;
    }
}

package company;

import helpers.interfaces.SubRouterProtocol;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.web.Router;

public enum CompanyRouter implements SubRouterProtocol {

    INSTANCE;

    @Override
    public Router router(Vertx vertx) {
        Router router = Router.router(vertx);

        // ── Public endpoints (no auth) ──────────────────────────────────────
        router.post("/register").handler(SelfRegisterCompanyController.INSTANCE::handle);
        router.post("/accept-invite").handler(AcceptCompanyInviteController.INSTANCE::handle);

        // ── Company Profile CRUD (admin/superadmin level — old routes kept for compat) ──
        router.post("/create").handler(CreateCompanyController.INSTANCE::handle);
        router.get("/list").handler(ListCompaniesController.INSTANCE::handle);
        router.get("/list/:id").handler(GetCompanyController.INSTANCE::handle);

        // ── Company Portal — authenticated COMPANY_HR routes ────────────────
        // Profile
        router.get("/me").handler(GetCompanyMeController.INSTANCE::handle);
        router.put("/me").handler(UpdateCompanyMeController.INSTANCE::handle);
        router.get("/me/permissions").handler(GetCompanyMyPermissionsController.INSTANCE::handle);

        // Drives
        router.get("/drives").handler(ListCompanyPortalDrivesController.INSTANCE::handle);
        router.get("/drives/:driveId/applications").handler(GetCompanyDriveApplicationsController.INSTANCE::handle);

        // Team management
        router.get("/team").handler(ListCompanyTeamController.INSTANCE::handle);
        router.post("/team/invite").handler(InviteCompanyTeamMemberController.INSTANCE::handle);
        router.put("/team/:userId").handler(UpdateCompanyTeamMemberController.INSTANCE::handle);
        router.delete("/team/:userId").handler(RemoveCompanyTeamMemberController.INSTANCE::handle);

        // ── Linked colleges (kept for compat) ──
        router.get("/:companyId/colleges").handler(ListLinkedCollegesController.INSTANCE::handle);

        // ── Reports (CSV downloads) ──
        router.get("/:companyId/reports/drives").handler(CompanyDriveReportController.INSTANCE::handle);
        router.get("/:companyId/reports/drives/:driveId/applications").handler(CompanyApplicationReportController.INSTANCE::handle);

        return router;
    }
}

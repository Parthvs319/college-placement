package superadmin;

import helpers.interfaces.SubRouterProtocol;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.web.Router;

import superadmin.aishe.AisheSubRouter;
import superadmin.analytics.PlatformActivityController;
import superadmin.analytics.PlatformAnalyticsController;
import superadmin.college.AnalyzeContractController;
import superadmin.college.CollegeSubRouter;
import superadmin.college.ContractRenewalsController;
import superadmin.college.GstinLookupController;
import superadmin.college.InvoiceDueController;
import superadmin.company.CompanySubRouter;
import superadmin.company.GetCompanyHrDetailController;
import superadmin.drive.DriveSubRouter;
import superadmin.drive.ListAllApplicationsController;
import superadmin.drive.ListAllOffersController;
import superadmin.email.SendActionOtpController;
import superadmin.email.SendAdminMailController;
import superadmin.email.SendOtpController;
import superadmin.email.SendPromoEmailController;
import superadmin.email.VerifyOtpController;
import superadmin.student.ListAllSubscriptionsController;
import superadmin.student.StudentSubRouter;
import superadmin.support.SupportSubRouter;
import superadmin.user.GetTpoDetailController;
import superadmin.user.UserSubRouter;

public enum SuperAdminRouter implements SubRouterProtocol {

    INSTANCE;

    @Override
    public Router router(Vertx vertx) {
        System.out.println("Here4");
        Router router = Router.router(vertx);

        // ── Platform Analytics & Activity ──
        router.get("/analytics").handler(PlatformAnalyticsController.INSTANCE::handle);
        router.get("/activity").handler(PlatformActivityController.INSTANCE::handle);

        // ── College-related root routes (no /colleges prefix) ──
        router.get("/contract-renewals").handler(ContractRenewalsController.INSTANCE::handle);
        router.get("/invoice-due").handler(InvoiceDueController.INSTANCE::handle);
        router.post("/analyze-contract").handler(AnalyzeContractController.INSTANCE::handle);
        router.get("/gstin/:gstin").handler(GstinLookupController.INSTANCE::handle);

        // ── Colleges sub-router ──
        router.route("/colleges*").subRouter(CollegeSubRouter.INSTANCE.router(vertx));

        // ── AISHE sub-router ──
        router.route("/aishe*").subRouter(AisheSubRouter.INSTANCE.router(vertx));

        // ── Email & OTP actions ──
        router.post("/send-admin-mail").handler(SendAdminMailController.INSTANCE::handle);
        router.post("/send-promo-email").handler(SendPromoEmailController.INSTANCE::handle);
        router.post("/send-otp").handler(SendOtpController.INSTANCE::handle);
        router.post("/verify-otp").handler(VerifyOtpController.INSTANCE::handle);
        router.post("/send-action-otp").handler(SendActionOtpController.INSTANCE::handle);

        // ── Students sub-router ──
        router.route("/students*").subRouter(StudentSubRouter.INSTANCE.router(vertx));

        // ── User lookup (no /users prefix) ──
        router.get("/tpo/:userId").handler(GetTpoDetailController.INSTANCE::handle);
        router.get("/company-hr/:userId").handler(GetCompanyHrDetailController.INSTANCE::handle);

        // ── Companies sub-router ──
        router.route("/companies*").subRouter(CompanySubRouter.INSTANCE.router(vertx));

        // ── Drives sub-router ──
        router.route("/drives*").subRouter(DriveSubRouter.INSTANCE.router(vertx));

        // ── Applications & Offers (no /drives prefix) ──
        router.get("/applications").handler(ListAllApplicationsController.INSTANCE::handle);
        router.get("/offers").handler(ListAllOffersController.INSTANCE::handle);

        // ── Users sub-router ──
        router.route("/users*").subRouter(UserSubRouter.INSTANCE.router(vertx));

        // ── Subscriptions & Credits (stay in superadmin root) ──
        router.get("/subscriptions").handler(ListAllSubscriptionsController.INSTANCE::handle);
        router.get("/subscriptions/:subscriptionId/credits").handler(CreditHistoryController.INSTANCE::handle);
        router.post("/subscriptions/:subscriptionId/top-up").handler(CreditTopUpController.INSTANCE::handle);

        // ── Support Tickets sub-router ──
        router.route("/support*").subRouter(SupportSubRouter.INSTANCE.router(vertx));

        return router;
    }
}

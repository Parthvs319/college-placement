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
        router.get("/activity").handler(PlatformActivityController.INSTANCE::handle);

        router.get("/contract-renewals").handler(ContractRenewalsController.INSTANCE::handle);
        router.get("/invoice-due").handler(InvoiceDueController.INSTANCE::handle);
        router.get("/colleges").handler(ListAllCollegesController.INSTANCE::handle);
        router.post("/colleges").handler(ctx -> {
            System.out.println("[DEBUG] POST /colleges hit!");
            CreateCollegeController.INSTANCE.handle(ctx);
        });
        router.post("/colleges/export").handler(ExportCollegesController.INSTANCE::handle);
        router.get("/colleges/:collegeId").handler(GetCollegeDetailController.INSTANCE::handle);
        router.post("/colleges/:collegeId/verify").handler(VerifyCollegeController.INSTANCE::handle);
        router.post("/colleges/:collegeId/toggle-active").handler(ToggleCollegeActiveController.INSTANCE::handle);

        router.get("/aishe/all").handler(AisheAllController.INSTANCE::handle);
        router.get("/aishe/search").handler(AisheSearchController.INSTANCE::handle);
        router.get("/gstin/:gstin").handler(GstinLookupController.INSTANCE::handle);

        router.post("/send-admin-mail").handler(SendAdminMailController.INSTANCE::handle);
        router.post("/send-promo-email").handler(SendPromoEmailController.INSTANCE::handle);
        router.post("/send-otp").handler(SendOtpController.INSTANCE::handle);
        router.post("/verify-otp").handler(VerifyOtpController.INSTANCE::handle);
        router.post("/send-action-otp").handler(SendActionOtpController.INSTANCE::handle);

        router.post("/analyze-contract").handler(AnalyzeContractController.INSTANCE::handle);
        router.post("/colleges/:collegeId/analyze-contract").handler(AnalyzeContractController.INSTANCE::handle);
        router.post("/colleges/:collegeId/tpo").handler(AddTpoController.INSTANCE::handle);
        router.post("/colleges/:collegeId/contract").handler(UploadContractController.INSTANCE::handle);
        router.get("/colleges/:collegeId/contract").handler(GetContractController.INSTANCE::handle);

        router.post("/colleges/:collegeId/invoice").handler(GenerateInvoiceController.INSTANCE::handle);
        router.get("/colleges/:collegeId/invoices").handler(ListInvoicesController.INSTANCE::handle);
        router.post("/colleges/:collegeId/invoices/:invoiceId/mark-paid").handler(MarkInvoicePaidController.INSTANCE::handle);

        router.get("/students").handler(ListAllStudentsController.INSTANCE::handle);
        router.get("/students/:studentId").handler(GetStudentDetailController.INSTANCE::handle);

        router.get("/tpo/:userId").handler(GetTpoDetailController.INSTANCE::handle);
        router.get("/company-hr/:userId").handler(GetCompanyHrDetailController.INSTANCE::handle);

        router.get("/companies").handler(ListAllCompaniesController.INSTANCE::handle);
        router.get("/companies/:companyId").handler(GetCompanyDetailController.INSTANCE::handle);
        router.post("/companies/:companyId/approve").handler(ApproveCompanyController.INSTANCE::handle);

        router.get("/drives").handler(ListAllDrivesController.INSTANCE::handle);
        router.get("/drives/:driveId").handler(GetDriveDetailController.INSTANCE::handle);

        router.get("/applications").handler(ListAllApplicationsController.INSTANCE::handle);

        router.get("/offers").handler(ListAllOffersController.INSTANCE::handle);

        router.get("/users").handler(ListAllUsersController.INSTANCE::handle);
        router.post("/users/:userId/toggle-active").handler(ToggleUserActiveController.INSTANCE::handle);

        router.get("/subscriptions").handler(ListAllSubscriptionsController.INSTANCE::handle);
        router.get("/subscriptions/:subscriptionId/credits").handler(CreditHistoryController.INSTANCE::handle);
        router.post("/subscriptions/:subscriptionId/top-up").handler(CreditTopUpController.INSTANCE::handle);

        // ── Support Tickets ──
        router.get("/support/tickets").handler(ListSupportTicketsController.INSTANCE::handle);
        router.put("/support/tickets/:ticketId").handler(UpdateSupportTicketController.INSTANCE::handle);

        return router;
    }
}

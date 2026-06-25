package college;

import helpers.interfaces.SubRouterProtocol;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.web.Router;

public enum CollegeRouter implements SubRouterProtocol {

    INSTANCE;

    @Override
    public Router router(Vertx vertx) {
        Router router = Router.router(vertx);

        // ── College CRUD ──
        router.post("/create").handler(CreateCollegeController.INSTANCE::handle);
        router.get("/list").handler(ListCollegesController.INSTANCE::handle);
        router.get("/me").handler(GetCollegeController.INSTANCE::handle);
        router.get("/me/permissions").handler(GetMyPermissionsController.INSTANCE::handle);
        router.put("/me").handler(UpdateCollegeController.INSTANCE::handle);

        // ── Placement Policy ──
        router.post("/policy").handler(CreatePolicyController.INSTANCE::handle);
        router.get("/policy").handler(GetPolicyController.INSTANCE::handle);

        // ── Documents ──
        router.post("/documents").handler(UploadDocumentController.INSTANCE::handle);
        router.get("/documents").handler(ListDocumentsController.INSTANCE::handle);

        // ── Analytics ──
        router.get("/analytics").handler(CollegeAnalyticsController.INSTANCE::handle);

        // ── Student Management (TPO/Admin) ──
        router.get("/students").handler(ListStudentsController.INSTANCE::handle);
        router.get("/students/placed").handler(ListPlacedStudentsController.INSTANCE::handle);
        router.get("/students/unplaced").handler(ListUnplacedStudentsController.INSTANCE::handle);
        router.get("/students/unverified").handler(ListUnverifiedStudentsController.INSTANCE::handle);
        router.post("/students/:studentId/verify").handler(VerifyStudentController.INSTANCE::handle);

        // ── Company Management (TPO links/onboards companies) ──
        router.post("/companies/onboard").handler(OnboardCompanyController.INSTANCE::handle);
        router.post("/companies/link").handler(LinkCompanyCollegeController.INSTANCE::handle);
        router.get("/companies").handler(ListCompanyCollegesController.INSTANCE::handle);
        router.post("/companies/remind-inactive").handler(RemindInactiveCompaniesController.INSTANCE::handle);

        // ── Drive Management (TPO/Admin) ──
        router.post("/drives").handler(CreateDriveController.INSTANCE::handle);
        router.get("/drives").handler(ListDrivesController.INSTANCE::handle);
        router.get("/drives/upcoming").handler(ListUpcomingDrivesController.INSTANCE::handle);
        router.put("/drives/:driveId").handler(UpdateDriveController.INSTANCE::handle);

        // ── Drive Applications & Rounds (TPO/Admin) ──
        router.get("/drives/:driveId/applications").handler(ListDriveApplicationsController.INSTANCE::handle);
        router.post("/drives/:driveId/rounds").handler(CreateRoundController.INSTANCE::handle);
        router.get("/drives/:driveId/rounds").handler(ListRoundsController.INSTANCE::handle);
        router.post("/rounds/:roundId/results").handler(SubmitRoundResultsController.INSTANCE::handle);

        // ── Offers (TPO/Admin) ──
        router.post("/drives/:driveId/offers").handler(CreateOfferController.INSTANCE::handle);
        router.get("/drives/:driveId/offers").handler(ListDriveOffersController.INSTANCE::handle);

        // ── Notifications (TPO/Admin) ──
        router.post("/notifications/send").handler(SendNotificationController.INSTANCE::handle);
        router.get("/notifications").handler(ListNotificationsController.INSTANCE::handle);

        router.post("/students/invite").handler(InviteStudentsController.INSTANCE::handle);
        router.post("/students/bulk-upload").handler(BulkUploadStudentsController.INSTANCE::handle);
        router.post("/students/onboarding-complete").handler(OnboardingCompleteController.INSTANCE::handle);

        // ── Bulk Operations ──
        router.post("/students/verify-bulk").handler(BulkVerifyStudentsController.INSTANCE::handle);

        // ── Team Management (primary TPO only) ──
        router.get("/team").handler(ListCollegeTeamController.INSTANCE::handle);
        router.post("/team/invite").handler(InviteTeamMemberController.INSTANCE::handle);
        router.put("/team/:userId").handler(UpdateTeamMemberController.INSTANCE::handle);
        router.delete("/team/:userId").handler(RemoveTeamMemberController.INSTANCE::handle);

        // ── Quick-Action Emails ──
        router.post("/drives/:driveId/remind-non-applicants").handler(RemindNonApplicantsController.INSTANCE::handle);
        router.post("/students/warn").handler(WarnStudentsController.INSTANCE::handle);

        // ── Support Tickets (TPO raises tickets) ──
        router.post("/support/tickets").handler(CreateSupportTicketController.INSTANCE::handle);

        // ── Reports (CSV downloads) ──
        router.get("/reports/students").handler(StudentReportController.INSTANCE::handle);
        router.get("/reports/drives").handler(DriveReportController.INSTANCE::handle);
        router.get("/reports/offers").handler(OfferReportController.INSTANCE::handle);

        router.post("/upload").handler(FileUploadController.INSTANCE::handle);

        return router;
    }
}

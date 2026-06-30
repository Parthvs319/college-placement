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

        // ── College Mail ──
        router.post("/send-mail").handler(college.email.SendCollegeMailController.INSTANCE::handle);

        // ── Support Tickets ──
        router.post("/support/tickets").handler(CreateSupportTicketController.INSTANCE::handle);

        // ── File Upload ──
        router.post("/upload").handler(FileUploadController.INSTANCE::handle);

        // ── Rounds (not under /drives — paths like /rounds/:roundId/...) ──
        router.get("/rounds/:roundId/results").handler(college.drive.ListRoundResultsController.INSTANCE::handle);
        router.post("/rounds/:roundId/results").handler(college.drive.SubmitRoundResultsController.INSTANCE::handle);
        router.post("/rounds/:roundId/complete").handler(college.drive.MarkRoundCompleteController.INSTANCE::handle);

        // ── Reports (CSV downloads) ──
        router.get("/reports/students").handler(college.student.StudentReportController.INSTANCE::handle);
        router.get("/reports/drives").handler(college.drive.DriveReportController.INSTANCE::handle);
        router.get("/reports/offers").handler(college.drive.OfferReportController.INSTANCE::handle);

        // ── Sub-routers ──
        // Using "/prefix*" (no slash before *) so both "/prefix" and "/prefix/..." are matched
        router.route("/students*").subRouter(college.student.StudentSubRouter.INSTANCE.router(vertx));
        router.route("/companies*").subRouter(college.company.CompanySubRouter.INSTANCE.router(vertx));
        router.route("/drives*").subRouter(college.drive.DriveSubRouter.INSTANCE.router(vertx));
        router.route("/team*").subRouter(college.team.TeamSubRouter.INSTANCE.router(vertx));
        router.route("/notifications*").subRouter(college.notification.NotificationSubRouter.INSTANCE.router(vertx));

        return router;
    }
}

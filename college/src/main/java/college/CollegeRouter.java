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

        // ── Company Management (TPO links companies) ──
        router.post("/companies/link").handler(LinkCompanyCollegeController.INSTANCE::handle);
        router.get("/companies").handler(ListCompanyCollegesController.INSTANCE::handle);

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

        return router;
    }
}

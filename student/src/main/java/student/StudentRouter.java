package student;

import helpers.interfaces.SubRouterProtocol;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.web.Router;

public enum StudentRouter implements SubRouterProtocol {

    INSTANCE;

    @Override
    public Router router(Vertx vertx) {
        Router router = Router.router(vertx);

        router.post("/onboard").handler(StudentOnboardController.INSTANCE::handle);

        router.get("/me").handler(GetMyProfileController.INSTANCE::handle);
        router.put("/me").handler(UpdateProfileController.INSTANCE::handle);
        router.post("/me/submit-profile").handler(SubmitProfileController.INSTANCE::handle);
        router.post("/me/resume").handler(ResumeUploadController.INSTANCE::handle);
        router.get("/me/resumes").handler(ListResumesController.INSTANCE::handle);
        router.put("/me/resumes/:resumeId/primary").handler(SetPrimaryResumeController.INSTANCE::handle);
        router.delete("/me/resumes/:resumeId").handler(DeleteResumeController.INSTANCE::handle);

        router.get("/me/applications").handler(MyApplicationsController.INSTANCE::handle);
        router.get("/me/offers").handler(MyOffersController.INSTANCE::handle);
        router.post("/me/offers/:offerId/respond").handler(RespondToOfferController.INSTANCE::handle);

        router.get("/drives").handler(ListAvailableDrivesController.INSTANCE::handle);
        router.get("/drives/:driveId").handler(GetDriveDetailController.INSTANCE::handle);
        router.post("/drives/:driveId/apply").handler(ApplyToDriveController.INSTANCE::handle);

        router.get("/pyq/company/:companyId").handler(GetPYQController.INSTANCE::handle);
        router.post("/pyq/contribute").handler(ContributePYQController.INSTANCE::handle);

        router.get("/premium/ats-score").handler(AtsScoreController.INSTANCE::handle);
        router.get("/premium/match/:driveId").handler(JdMatchController.INSTANCE::handle);
        router.post("/premium/resume-improve").handler(ResumeImproveController.INSTANCE::handle);
        router.post("/premium/generate-resume").handler(GenerateResumeController.INSTANCE::handle);
        router.post("/premium/parse-resume").handler(ParseResumeController.INSTANCE::handle);

        return router;
    }
}

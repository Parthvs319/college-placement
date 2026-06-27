package superadmin.college;

import helpers.interfaces.SubRouterProtocol;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.web.Router;

public enum CollegeSubRouter implements SubRouterProtocol {

    INSTANCE;

    @Override
    public Router router(Vertx vertx) {
        Router router = Router.router(vertx);

        // Static paths before parameterised paths
        router.get("/export").handler(ExportCollegesController.INSTANCE::handle);
        router.post("/export").handler(ExportCollegesController.INSTANCE::handle);
        router.get("/").handler(ListAllCollegesController.INSTANCE::handle);
        router.post("/").handler(ctx -> {
            System.out.println("[DEBUG] POST /colleges hit!");
            CreateCollegeController.INSTANCE.handle(ctx);
        });

        // Parameterised paths
        router.get("/:collegeId").handler(GetCollegeDetailController.INSTANCE::handle);
        router.post("/:collegeId/verify").handler(VerifyCollegeController.INSTANCE::handle);
        router.post("/:collegeId/toggle-active").handler(ToggleCollegeActiveController.INSTANCE::handle);
        router.post("/:collegeId/analyze-contract").handler(AnalyzeContractController.INSTANCE::handle);
        router.post("/:collegeId/tpo").handler(AddTpoController.INSTANCE::handle);
        router.post("/:collegeId/contract").handler(UploadContractController.INSTANCE::handle);
        router.get("/:collegeId/contract").handler(GetContractController.INSTANCE::handle);
        router.post("/:collegeId/invoice").handler(GenerateInvoiceController.INSTANCE::handle);
        router.get("/:collegeId/invoices").handler(ListInvoicesController.INSTANCE::handle);
        router.post("/:collegeId/invoices/:invoiceId/mark-paid").handler(MarkInvoicePaidController.INSTANCE::handle);

        return router;
    }
}

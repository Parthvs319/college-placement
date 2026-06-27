package superadmin.student;

import helpers.interfaces.SubRouterProtocol;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.web.Router;

public enum StudentSubRouter implements SubRouterProtocol {

    INSTANCE;

    @Override
    public Router router(Vertx vertx) {
        Router router = Router.router(vertx);

        router.get("/").handler(ListAllStudentsController.INSTANCE::handle);
        router.get("/:studentId").handler(GetStudentDetailController.INSTANCE::handle);

        return router;
    }
}

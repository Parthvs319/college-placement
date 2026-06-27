package college.team;

import helpers.interfaces.SubRouterProtocol;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.web.Router;

public enum TeamSubRouter implements SubRouterProtocol {

    INSTANCE;

    @Override
    public Router router(Vertx vertx) {
        Router router = Router.router(vertx);

        router.get("/").handler(ListCollegeTeamController.INSTANCE::handle);
        router.post("/invite").handler(InviteTeamMemberController.INSTANCE::handle);
        router.put("/:userId").handler(UpdateTeamMemberController.INSTANCE::handle);
        router.delete("/:userId").handler(RemoveTeamMemberController.INSTANCE::handle);

        return router;
    }
}

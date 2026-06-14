package user;

import helpers.interfaces.SubRouterProtocol;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.web.Router;

public enum UserRouter implements SubRouterProtocol {

    INSTANCE;

    @Override
    public Router router(Vertx vertx) {
        Router router = Router.router(vertx);
        router.get("/getAllUsers").handler(GetAllUsersController.INSTANCE::handle);
        router.get("/:type/getAllUsers").handler(GetAllUsersByTypeController.INSTANCE::handle);
        router.post("/signUp").handler(UserSignUpController.INSTANCE::handle);
        router.post("/login").handler(UserLoginController.INSTANCE::handle);
        router.post("/register/invite").handler(StudentRegisterByInviteController.INSTANCE::handle);
        router.post("/deactivateUser").handler(DeactivateUserController.INSTANCE::handle);
        router.post("/activateUser").handler(ActivateUserController.INSTANCE::handle);

//        router.get("/").handler(GetAllCitiesController.INSTANCE::handle);
//        router.delete("/:id").handler(.INSTANCE::handle);
//        router.post("/removeField").handler(.INSTANCE::handle);
        return router;
    }

}

package user;

import helpers.annotations.UserAnnotation;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import lombok.Data;
import models.access.middlewear.user.UserAccessMiddleware;
import models.body.UserLoginRequest;
import java.util.ArrayList;
import java.util.List;
import models.enums.UserType;
import models.repos.UserRepository;

@UserAnnotation
public enum GetAllUsersController implements BaseController {

    INSTANCE;

    @Override
    public void handle(RoutingContext event) {
        UserAccessMiddleware.INSTANCE.with(event , new ArrayList<>() ,  this.getClass())
                .map(this::map)
                .subscribe(
                        o -> ResponseUtils.INSTANCE.writeJsonResponse(event, o),
                        error -> {
                            ResponseUtils.INSTANCE.handleError(event, error);
                        }
                );
    }

    private Response map(UserLoginRequest request) {
        if (!request.getUser().getUserType().equals(UserType.SUPER_ADMIN)) {
            throw new RoutingError("You are not permitted to access this data !");
        }
        Response response = new Response();
        UserRepository.INSTANCE.findAll().forEach(dbUser -> {
            User u = new User();
            u.setMobile(dbUser.getMobile());
            u.setName(dbUser.getName());
            u.setActive(dbUser.isActive());
            u.setEmail(dbUser.getEmail());

            u.setUserType(dbUser.getUserType());
            u.setVerified(dbUser.isVerified());
            response.getUsers().add(u);
        });
        return response;
    }

    @Data
    class Response {
        List<User> users = new ArrayList<>();
    }

    @Data
    class User {
        public String mobile;

        public String name;

        public String email;

        public String password;

        public boolean active;

        public UserType userType;

        public boolean verified = false;
    }
}


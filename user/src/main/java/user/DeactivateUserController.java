package user;

import helpers.annotations.UserAnnotation;
import helpers.blueprint.enums.RequestItemType;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.RequestItem;
import helpers.utils.ResponseUtils;
import helpers.utils.SuccessResponse;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.user.UserAccessMiddleware;
import models.body.UserLoginRequest;
import models.enums.UserType;
import models.repos.UserRepository;
import models.sql.User;

import java.util.ArrayList;
import java.util.List;

@UserAnnotation
public enum DeactivateUserController implements BaseController {

    INSTANCE;

    public List<RequestItem> items() {
        List<helpers.utils.RequestItem> items = new ArrayList<>();
        items.add(RequestItem.builder().key("userId").itemType(RequestItemType.INTEGER).required(false).build());
        return items;
    }

    @Override
    public void handle(RoutingContext event) {
        UserAccessMiddleware.INSTANCE.with(event , items() ,  this.getClass())
                .map(this::map)
                .subscribe(
                        o -> ResponseUtils.INSTANCE.writeJsonResponse(event, o),
                        error -> {
                            ResponseUtils.INSTANCE.handleError(event, error);
                        }
                );
    }

    private SuccessResponse map(UserLoginRequest request) {
        if (!request.getUser().getUserType().equals(UserType.SUPER_ADMIN)) {
            throw new RoutingError("You are not permitted to deactivate users !");
        }
        Long userId = request.getRequest().get("userId");
        User user = UserRepository.INSTANCE.byId(userId);
        if(user == null) {
            throw new RoutingError("Invalid User Id Passed !");
        } else {
            if(user.isActive()) {
                user.setActive(false);
                user.update();
            } else {
                throw new RoutingError("User is already deactivated!");
            }
        }
        return new SuccessResponse();
    }

}

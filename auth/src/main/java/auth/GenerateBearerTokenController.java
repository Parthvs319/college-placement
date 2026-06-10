package auth;

import helpers.blueprint.enums.RequestItemType;
import helpers.customErrors.RoutingError;
import helpers.interfaces.ParamsController;
import helpers.utils.RequestItem;
import helpers.utils.RequestZipped;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.core.RxHelper;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.repos.UserRepository;
import models.sql.User;
import models.access.tokens.BearerToken;
import models.access.tokens.TokenService;
import rx.Single;
import java.util.ArrayList;
import java.util.List;

public enum GenerateBearerTokenController implements ParamsController {

    INSTANCE ;

    @Override
    public void handle(RoutingContext event) {
        Single.just(event)
                .subscribeOn(RxHelper.blockingScheduler(event.vertx()))
                .map(this::map)
                .map(this::map)
                .subscribe(
                        o -> ResponseUtils.INSTANCE.writeJsonResponse(event, o),
                        error ->{
                            ResponseUtils.INSTANCE.handleError(event, error);
                        }
                );
    }

    private BearerToken map(RequestZipped requestZipped) {
        try {
            Long userId = requestZipped.getRequest().get("userId");
            User user = UserRepository.INSTANCE.byId(userId);
            if(user == null) {
                throw new RoutingError("Invalid user id !");
            }
            return TokenService.generateToken(userId , user.getEmail() , user.getUserType().getValue() , user.getName());
        } catch (Exception e) {
            e.printStackTrace();
            throw new RoutingError(e.getMessage());
        }
    }


    @Override
    public List<RequestItem> items() {
        List<RequestItem> items = new ArrayList<>();
        items.add(RequestItem.builder().key("userId").required(true).itemType(RequestItemType.INTEGER).build());
        return items;
    }
}

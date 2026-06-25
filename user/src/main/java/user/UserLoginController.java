package user;

import helpers.blueprint.enums.RequestItemType;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.*;
import io.vertx.rxjava.core.RxHelper;
import io.vertx.rxjava.ext.web.RoutingContext;
import lombok.Data;
import models.access.tokens.BearerToken;
import models.access.tokens.TokenService;
import models.repos.UserRepository;
import models.sql.User;
import rx.Single;

import java.util.ArrayList;
import java.util.List;

public enum UserLoginController implements BaseController {

    INSTANCE;

    public List<RequestItem> items() {
        List<helpers.utils.RequestItem> items = new ArrayList<>();
        items.add(RequestItem.builder().key("email").itemType(RequestItemType.STRING).required(false).build());
        items.add(RequestItem.builder().key("mobile").itemType(RequestItemType.STRING).required(false).build());
        items.add(RequestItem.builder().key("password").itemType(RequestItemType.STRING).required(false).build());
        items.add(RequestItem.builder().key("otp").itemType(RequestItemType.STRING).required(false).build());
        return items;
    }

    @Override
    public void handle(RoutingContext event) {
        Single.just(event)
                .subscribeOn(RxHelper.blockingScheduler(event.vertx()))
                .map(this::map).
                map(this::map)
                .subscribe(
                        o -> ResponseUtils.INSTANCE.writeJsonResponse(event, o),
                        error -> {
                            ResponseUtils.INSTANCE.handleError(event, error);
                        }
                );
    }

    private RequestZipped map(RoutingContext event){
        return RequestHelper.INSTANCE.requestZipped(event,items());
    }

    private Response map(RequestZipped request) {
        Response response = new Response();
        boolean isMobileLogin = true;
        User user = null;
        if(request.getRequest().isPresent("mobile")) {
            user = UserRepository.INSTANCE.byMobile(request.getRequest().get("mobile"));
        } else if(request.getRequest().isPresent("email")) {
            isMobileLogin = false;
            user = UserRepository.INSTANCE.byEmail(request.getRequest().get("email"));
        } else {
            throw new RuntimeException("Either mobile or email is mandatory to pass !");
        }
        if(user == null) {
            if(isMobileLogin)
                throw new RoutingError("Invalid Mobile Number Passed !");
            else
                throw new RoutingError("Invalid Email Passed !");
        } else {
            if(request.getRequest().isPresent("password")) {
                if(PasswordUtils.INSTANCE.match(request.getRequest().get("password") , user.getPassword())) {
                    response.setSuccess(true);
                    response.setBearerToken(TokenService.generateToken(user.getId() , user.getEmail() , user.getUserType().getValue() , user.getName()));
                    if (user.getUserType() == models.enums.UserType.TPO
                            || user.getUserType() == models.enums.UserType.COLLEGE_ADMIN
                            || user.getUserType() == models.enums.UserType.COMPANY_HR) {
                        response.setIsPrimary(user.isPrimary);
                    }
                } else {
                    throw new RoutingError("Invalid Password ! Please try with a valid password !");
                }
            } else if (request.getRequest().isPresent("otp")) {
                if(user.getCurrentOtp().equalsIgnoreCase(request.getRequest().get("otp"))) {
                    response.setSuccess(true);
                    response.setBearerToken(TokenService.generateToken(user.getId() , user.getEmail() , user.getUserType().getValue() , user.getName()));
                } else {
                    throw new RoutingError("Invalid Otp ! Please try with a valid otp !");
                }
            }
        }
        return response;
    }
    @Data
    class Response {
        boolean success = false;
        BearerToken bearerToken = null;
        Boolean isPrimary = null;     // set for TPO / COMPANY_HR; null for other user types
    }

}

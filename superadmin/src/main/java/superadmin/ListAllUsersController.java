package superadmin;

import helpers.annotations.SuperAdminRole;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.superadmin.SuperAdminAccessMiddleware;
import models.enums.UserType;
import models.repos.UserRepository;
import models.sql.User;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@SuperAdminRole
public enum ListAllUsersController implements BaseController {

    INSTANCE;

    @Override
    public void handle(RoutingContext event) {
        SuperAdminAccessMiddleware.INSTANCE.with(event, new ArrayList<>(), this.getClass())
                .map(req -> {
                    String typeParam = event.request().getParam("type");
                    String collegeIdParam = event.request().getParam("collegeId");

                    List<User> users;
                    if (collegeIdParam != null && !collegeIdParam.isEmpty()) {
                        Long cId = Long.parseLong(collegeIdParam);
                        if (typeParam != null && !typeParam.isEmpty()) {
                            users = UserRepository.INSTANCE.byCollegeAndType(cId, UserType.valueOf(typeParam));
                        } else {
                            users = UserRepository.INSTANCE.byCollege(cId);
                        }
                    } else {
                        users = UserRepository.INSTANCE.findAll();
                        if (typeParam != null && !typeParam.isEmpty()) {
                            UserType filterType = UserType.valueOf(typeParam);
                            users = users.stream().filter(u -> u.userType == filterType).collect(Collectors.toList());
                        }
                    }

                    return users.stream().map(u -> {
                        SuperAdminDtos.UserSummary s = new SuperAdminDtos.UserSummary();
                        s.setId(u.getId());
                        s.setName(u.name);
                        s.setEmail(u.email);
                        s.setMobile(u.mobile);
                        s.setUserType(u.userType.getValue());
                        s.setVerified(u.verified);
                        s.setActive(u.active);
                        if (u.college != null) {
                            s.setCollegeName(u.college.name);
                            s.setCollegeId(u.college.getId());
                        }
                        return s;
                    }).collect(Collectors.toList());
                })
                .subscribe(
                        o -> ResponseUtils.INSTANCE.writeJsonResponse(event, o),
                        error -> ResponseUtils.INSTANCE.handleError(event, error)
                );
    }
}

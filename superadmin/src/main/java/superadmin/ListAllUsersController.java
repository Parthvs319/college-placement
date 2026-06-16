package superadmin;

import helpers.annotations.SuperAdminRole;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.superadmin.SuperAdminAccessMiddleware;
import models.enums.UserType;
import models.repos.CompanyCollegeRepository;
import models.repos.StudentRepository;
import models.repos.UserRepository;
import models.sql.CompanyCollege;
import models.sql.Student;
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
                        if (u.getCreatedAt() != null) {
                            s.setCreatedAt(u.getCreatedAt().toString());
                        }
                        if (u.getCollege() != null) {
                            s.setCollegeName(u.getCollege().getName());
                            s.setCollegeId(u.getCollege().getId());
                        }
                        if (u.userType == UserType.STUDENT) {
                            Student st = StudentRepository.INSTANCE.byUserId(u.getId());
                            if (st != null) s.setStudentId(st.getId());
                        }
                        if (u.userType == UserType.COMPANY_HR) {
                            List<CompanyCollege> managed = CompanyCollegeRepository.INSTANCE.byManagedUser(u.getId());
                            if (!managed.isEmpty()) {
                                String names = managed.stream()
                                        .filter(cc -> cc.company != null)
                                        .map(cc -> cc.company.name)
                                        .distinct()
                                        .collect(Collectors.joining(", "));
                                if (!names.isEmpty()) s.setCompanyName(names);
                            }
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

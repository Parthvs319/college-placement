package superadmin;

import helpers.annotations.SuperAdminRole;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.superadmin.SuperAdminAccessMiddleware;
import models.repos.DriveApplicationRepository;
import models.repos.OfferRepository;
import models.repos.StudentRepository;
import models.sql.Student;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@SuperAdminRole(request = {"collegeId:i@optional", "department:s@optional", "placed:s@optional"})
public enum ListAllStudentsController implements BaseController {

    INSTANCE;

    @Override
    public void handle(RoutingContext event) {
        SuperAdminAccessMiddleware.INSTANCE.with(event, new ArrayList<>(), this.getClass())
                .map(req -> {
                    // Optional filters from query params
                    String collegeIdParam = event.request().getParam("collegeId");
                    String deptParam = event.request().getParam("department");
                    String placedParam = event.request().getParam("placed");

                    var query = StudentRepository.INSTANCE.where();

                    if (collegeIdParam != null && !collegeIdParam.isEmpty()) {
                        query.eq("college.id", Long.parseLong(collegeIdParam));
                    }
                    if (deptParam != null && !deptParam.isEmpty()) {
                        query.eq("department", deptParam);
                    }
                    if (placedParam != null) {
                        query.eq("placed", Boolean.parseBoolean(placedParam));
                    }

                    List<Student> students = query.orderBy("createdAt desc").findList();

                    return students.stream().map(s -> {
                        SuperAdminDtos.StudentOverview o = new SuperAdminDtos.StudentOverview();
                        o.setId(s.getId());
                        o.setName(s.user != null ? s.user.name : null);
                        o.setEmail(s.user != null ? s.user.email : null);
                        o.setDepartment(s.department);
                        o.setCgpa(s.cgpa);
                        o.setCollegeName(s.college != null ? s.college.name : null);
                        o.setCollegeId(s.college != null ? s.college.getId() : null);
                        o.setVerified(s.user != null && s.user.verified);
                        o.setPlaced(s.placed);
                        o.setApplicationCount(DriveApplicationRepository.INSTANCE.byStudent(s.getId()).size());
                        o.setOfferCount(OfferRepository.INSTANCE.byStudent(s.getId()).size());
                        return o;
                    }).collect(Collectors.toList());
                })
                .subscribe(
                        o -> ResponseUtils.INSTANCE.writeJsonResponse(event, o),
                        error -> ResponseUtils.INSTANCE.handleError(event, error)
                );
    }
}

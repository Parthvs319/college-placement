package superadmin.student;

import superadmin.SuperAdminDtos;

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

                    var query = StudentRepository.INSTANCE.whereWithFetch();

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
                        o.setName(s.getUser() != null ? s.getUser().getName() : null);
                        o.setEmail(s.getUser() != null ? s.getUser().getEmail() : null);
                        o.setDepartment(s.getDepartment());
                        o.setCgpa(s.getCgpa());
                        o.setCollegeName(s.getCollege() != null ? s.getCollege().getName() : null);
                        o.setCollegeId(s.getCollege() != null ? s.getCollege().getId() : null);
                        o.setVerified(s.getUser() != null && s.getUser().isVerified());
                        o.setPlaced(s.isPlaced());
                        o.setPlacedAt(s.getPlacedAt() != null ? s.getPlacedAt().toString() : null);
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

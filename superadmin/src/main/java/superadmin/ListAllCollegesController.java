package superadmin;

import helpers.annotations.SuperAdminRole;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.ebean.DB;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.superadmin.SuperAdminAccessMiddleware;
import models.repos.CompanyCollegeRepository;
import models.repos.CollegeRepository;
import models.repos.DriveRepository;
import models.repos.StudentRepository;
import models.sql.City;
import models.sql.College;
import models.sql.States;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@SuperAdminRole
public enum ListAllCollegesController implements BaseController {

    INSTANCE;

    @Override
    public void handle(RoutingContext event) {
        SuperAdminAccessMiddleware.INSTANCE.with(event, new ArrayList<>(), this.getClass())
                .map(req -> {
                    List<College> colleges = CollegeRepository.INSTANCE.findAll();
                    return colleges.stream().map(c -> {
                        SuperAdminDtos.CollegeSummary s = new SuperAdminDtos.CollegeSummary();
                        s.setId(c.getId());
                        s.setName(c.name);
                        s.setCode(c.code);
                        if (c.cityId != null) {
                            City ct = DB.find(City.class, c.cityId);
                            if (ct != null) s.setCity(ct.name);
                        }
                        if (c.stateId != null) {
                            States st = DB.find(States.class, c.stateId);
                            if (st != null) s.setState(st.name);
                        }
                        s.setUniversity(c.university);
                        s.setVerified(c.verified);
                        s.setActive(c.active);

                        int totalStudents = StudentRepository.INSTANCE.byCollege(c.getId()).size();
                        int placedStudents = StudentRepository.INSTANCE.findPlaced(c.getId()).size();
                        s.setStudentCount(totalStudents);
                        s.setPlacedCount(placedStudents);
                        s.setDriveCount(DriveRepository.INSTANCE.byCollege(c.getId()).size());
                        s.setCompanyCount(CompanyCollegeRepository.INSTANCE.byCollege(c.getId()).size());
                        s.setPlacementRate(totalStudents > 0 ? (double) placedStudents / totalStudents * 100 : 0);
                        return s;
                    }).collect(Collectors.toList());
                })
                .subscribe(
                        o -> ResponseUtils.INSTANCE.writeJsonResponse(event, o),
                        error -> ResponseUtils.INSTANCE.handleError(event, error)
                );
    }
}

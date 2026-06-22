package superadmin;

import helpers.annotations.SuperAdminRole;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.ebean.DB;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.superadmin.SuperAdminAccessMiddleware;
import models.repos.CompanyCollegeRepository;
import models.repos.CollegeContractRepository;
import models.repos.CollegeRepository;
import models.repos.DriveRepository;
import models.repos.StudentRepository;
import models.sql.City;
import models.sql.College;
import models.sql.CollegeContract;
import models.sql.CompanyCollege;
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
                        s.setName(c.getName());
                        s.setCode(c.getCode());
                        if (c.getCityId() != null) {
                            City ct = DB.find(City.class, c.getCityId());
                            if (ct != null) s.setCity(ct.getName());
                        }
                        if (c.getStateId() != null) {
                            States st = DB.find(States.class, c.getStateId());
                            if (st != null) s.setState(st.getName());
                        }
                        s.setUniversity(c.getUniversity());
                        s.setVerified(c.isVerified());
                        s.setActive(c.isActive());

                        int totalStudents = StudentRepository.INSTANCE.byCollege(c.getId()).size();
                        int placedStudents = StudentRepository.INSTANCE.findPlaced(c.getId()).size();
                        s.setStudentCount(totalStudents);
                        s.setPlacedCount(placedStudents);
                        s.setDriveCount(DriveRepository.INSTANCE.byCollege(c.getId()).size());
                        List<CompanyCollege> companyColleges = CompanyCollegeRepository.INSTANCE.byCollege(c.getId());
                        s.setCompanyCount(companyColleges.size());
                        s.setStartupCount((int) companyColleges.stream()
                                .filter(cc -> cc.getCompany() != null && cc.getCompany().isStartup())
                                .count());
                        s.setPlacementRate(totalStudents > 0 ? (double) placedStudents / totalStudents * 100 : 0);

                        // Verification + GSTIN
                        s.setGstin(c.gstin);
                        s.setTpoName(c.tpoName);
                        s.setEmailVerified(c.isEmailVerified);
                        s.setPhoneVerified(c.isPhoneVerified);

                        // Active contract summary
                        CollegeContract contract = CollegeContractRepository.INSTANCE.latestActive(c.getId());
                        if (contract != null) {
                            s.setContractEndDate(contract.getValidTo());
                            s.setContractType(contract.getContractType());
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

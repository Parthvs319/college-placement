package superadmin;

import helpers.annotations.SuperAdminRole;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.ebean.DB;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.superadmin.SuperAdminAccessMiddleware;
import models.repos.*;
import models.sql.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@SuperAdminRole
public enum GetCollegeDetailController implements BaseController {

    INSTANCE;

    @Override
    public void handle(RoutingContext event) {
        SuperAdminAccessMiddleware.INSTANCE.with(event, new ArrayList<>(), this.getClass())
                .map(req -> {
                    Long collegeId = Long.parseLong(event.pathParam("collegeId"));
                    College college = CollegeRepository.INSTANCE.byId(collegeId);
                    if (college == null) throw new RoutingError(404, "College not found");

                    Map<String, Object> result = new HashMap<>();
                    result.put("id", college.getId());
                    result.put("name", college.getName());
                    result.put("code", college.getCode());
                    result.put("university", college.getUniversity());
                    result.put("address", college.getAddress());
                    result.put("cityId", college.getCityId());
                    result.put("stateId", college.getStateId());
                    String cityName = null;
                    String stateName = null;
                    if (college.getCityId() != null) {
                        City ct = DB.find(City.class, college.getCityId());
                        if (ct != null) cityName = ct.getName();
                    }
                    if (college.getStateId() != null) {
                        States st = DB.find(States.class, college.getStateId());
                        if (st != null) stateName = st.getName();
                    }
                    result.put("city", cityName);
                    result.put("state", stateName);
                    result.put("pincode", college.getPincode());
                    result.put("website", college.getWebsite());
                    result.put("logoUrl", college.getLogoUrl());
                    result.put("contactEmail", college.getContactEmail());
                    result.put("contactPhone", college.getContactPhone());
                    result.put("departments", college.getDepartments());
                    result.put("verified", college.isVerified());
                    result.put("active", college.isActive());

                    // Students
                    List<Student> allStudents = StudentRepository.INSTANCE.byCollege(collegeId);
                    List<Student> placedStudents = StudentRepository.INSTANCE.findPlaced(collegeId);
                    int total = allStudents.size();
                    int placed = placedStudents.size();
                    result.put("totalStudents", total);
                    result.put("placedStudents", placed);
                    result.put("unplacedStudents", total - placed);
                    result.put("placementRate", total > 0 ? (double) placed / total * 100 : 0);

                    // Drives
                    List<Drive> drives = DriveRepository.INSTANCE.byCollege(collegeId);
                    result.put("driveCount", drives.size());

                    // Companies
                    List<CompanyCollege> companyColleges = CompanyCollegeRepository.INSTANCE.byCollege(collegeId);
                    result.put("companyCount", companyColleges.size());

                    // Users linked to this college
                    result.put("users", UserRepository.INSTANCE.byCollege(collegeId).stream().map(u -> {
                        Map<String, Object> um = new HashMap<>();
                        um.put("id", u.getId());
                        um.put("name", u.getName());
                        um.put("email", u.getEmail());
                        um.put("userType", u.getUserType().getValue());
                        um.put("verified", u.isVerified());
                        um.put("active", u.isActive());
                        return um;
                    }).toList());

                    // Student list for modal (id, name, email, department, placed, cgpa)
                    result.put("studentList", allStudents.stream().map(s -> {
                        Map<String, Object> sm = new HashMap<>();
                        sm.put("id", s.getId());
                        sm.put("name", s.getUser() != null ? s.getUser().getName() : null);
                        sm.put("email", s.getUser() != null ? s.getUser().getEmail() : null);
                        sm.put("department", s.getDepartment());
                        sm.put("placed", s.isPlaced());
                        sm.put("cgpa", s.getCgpa());
                        sm.put("currentCtc", s.getCurrentCtc());
                        return sm;
                    }).toList());

                    // Company list for modal (id, name, industry, active)
                    result.put("companyList", companyColleges.stream().map(cc -> {
                        Map<String, Object> cm = new HashMap<>();
                        if (cc.getCompany() != null) {
                            cm.put("companyId", cc.getCompany().getId());
                            cm.put("name", cc.getCompany().getName());
                            cm.put("industry", cc.getCompany().getIndustry());
                            cm.put("startup", cc.getCompany().isStartup());
                        }
                        cm.put("active", cc.isActive());
                        return cm;
                    }).filter(cm -> cm.containsKey("companyId")).toList());

                    // Drive list for modal (id, title, companyName, status, ctcOffered, driveDate)
                    result.put("driveList", drives.stream().map(d -> {
                        Map<String, Object> dm = new HashMap<>();
                        dm.put("driveId", d.getId());
                        dm.put("title", d.getTitle());
                        dm.put("status", d.getStatus() != null ? d.getStatus().name() : null);
                        dm.put("ctcOffered", d.getCtcOffered());
                        dm.put("driveDate", d.getDriveDate() != null ? d.getDriveDate().toString() : null);
                        dm.put("employmentType", d.getEmploymentType() != null ? d.getEmploymentType().name() : null);
                        if (d.getCompanyCollege() != null && d.getCompanyCollege().getCompany() != null) {
                            dm.put("companyName", d.getCompanyCollege().getCompany().getName());
                        }
                        return dm;
                    }).toList());

                    return result;
                })
                .subscribe(
                        o -> ResponseUtils.INSTANCE.writeJsonResponse(event, o),
                        error -> ResponseUtils.INSTANCE.handleError(event, error)
                );
    }
}

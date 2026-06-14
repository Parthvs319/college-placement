package superadmin;

import helpers.annotations.SuperAdminRole;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.ebean.DB;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.superadmin.SuperAdminAccessMiddleware;
import models.repos.*;
import models.sql.City;
import models.sql.College;
import models.sql.States;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

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
                    result.put("name", college.name);
                    result.put("code", college.code);
                    result.put("university", college.university);
                    result.put("address", college.address);
                    result.put("cityId", college.cityId);
                    result.put("stateId", college.stateId);
                    String cityName = null;
                    String stateName = null;
                    if (college.cityId != null) {
                        City ct = DB.find(City.class, college.cityId);
                        if (ct != null) cityName = ct.name;
                    }
                    if (college.stateId != null) {
                        States st = DB.find(States.class, college.stateId);
                        if (st != null) stateName = st.name;
                    }
                    result.put("city", cityName);
                    result.put("state", stateName);
                    result.put("pincode", college.pincode);
                    result.put("website", college.website);
                    result.put("logoUrl", college.logoUrl);
                    result.put("contactEmail", college.contactEmail);
                    result.put("contactPhone", college.contactPhone);
                    result.put("departments", college.departments);
                    result.put("verified", college.verified);
                    result.put("active", college.active);

                    int total = StudentRepository.INSTANCE.byCollege(collegeId).size();
                    int placed = StudentRepository.INSTANCE.findPlaced(collegeId).size();
                    result.put("totalStudents", total);
                    result.put("placedStudents", placed);
                    result.put("unplacedStudents", total - placed);
                    result.put("placementRate", total > 0 ? (double) placed / total * 100 : 0);
                    result.put("driveCount", DriveRepository.INSTANCE.byCollege(collegeId).size());
                    result.put("companyCount", CompanyCollegeRepository.INSTANCE.byCollege(collegeId).size());

                    // Users linked to this college
                    result.put("users", UserRepository.INSTANCE.byCollege(collegeId).stream().map(u -> {
                        Map<String, Object> um = new HashMap<>();
                        um.put("id", u.getId());
                        um.put("name", u.name);
                        um.put("email", u.email);
                        um.put("userType", u.userType.getValue());
                        um.put("verified", u.verified);
                        um.put("active", u.active);
                        return um;
                    }).toList());

                    return result;
                })
                .subscribe(
                        o -> ResponseUtils.INSTANCE.writeJsonResponse(event, o),
                        error -> ResponseUtils.INSTANCE.handleError(event, error)
                );
    }
}

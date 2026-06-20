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
                        um.put("name", u.getName());
                        um.put("email", u.getEmail());
                        um.put("userType", u.getUserType().getValue());
                        um.put("verified", u.isVerified());
                        um.put("active", u.isActive());
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

package superadmin;

import helpers.annotations.SuperAdminRole;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.superadmin.SuperAdminAccessMiddleware;
import models.repos.*;
import models.sql.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.Data;

/**
 * GET /admin/company-hr/:userId
 * Returns detailed info about a Company HR user,
 * including managed companies, drives conducted, offers given, linked colleges.
 */
@SuperAdminRole
public enum GetCompanyHrDetailController implements BaseController {

    INSTANCE;

    @Override
    public void handle(RoutingContext event) {
        SuperAdminAccessMiddleware.INSTANCE.with(event, new ArrayList<>(), this.getClass())
                .map(req -> {
                    String idParam = event.request().getParam("userId");
                    if (idParam == null) throw new RoutingError("userId is required");
                    long userId = Long.parseLong(idParam);

                    User user = UserRepository.INSTANCE.byId(userId);
                    if (user == null) throw new RoutingError("User not found");

                    CompanyHrDetail detail = new CompanyHrDetail();

                    // Basic info
                    detail.id = user.getId();
                    detail.name = user.name;
                    detail.email = user.email;
                    detail.mobile = user.mobile;
                    detail.userType = user.userType.getValue();
                    detail.verified = user.verified;
                    detail.active = user.active;
                    detail.createdAt = user.getCreatedAt() != null ? user.getCreatedAt().toString() : null;

                    if (user.college != null) {
                        detail.collegeId = user.getCollege().getId();
                        detail.collegeName = user.getCollege().getName();
                    }

                    // Direct company link
                    if (user.company != null) {
                        detail.companyId = user.company.getId();
                        detail.companyName = user.company.name;
                    }

                    // Find company-college links managed by this HR
                    List<CompanyCollege> managed = CompanyCollegeRepository.INSTANCE.byManagedUser(userId);

                    // Companies
                    Map<Long, Company> companiesMap = new HashMap<>();
                    for (CompanyCollege cc : managed) {
                        if (cc.company != null) {
                            companiesMap.put(cc.company.getId(), cc.company);
                        }
                    }

                    detail.companies = companiesMap.values().stream().map(c -> {
                        CompanyInfo ci = new CompanyInfo();
                        ci.companyId = c.getId();
                        ci.name = c.getName();
                        ci.industry = c.getIndustry();
                        ci.website = c.getWebsite();
                        ci.headquarters = c.getHeadquarters();
                        ci.startup = c.isStartup();
                        ci.description = c.getDescription();
                        return ci;
                    }).collect(Collectors.toList());

                    // Linked colleges
                    Set<Long> collegeIds = managed.stream()
                            .filter(cc -> cc.college != null)
                            .map(cc -> cc.college.getId())
                            .collect(Collectors.toSet());

                    detail.linkedColleges = managed.stream()
                            .filter(cc -> cc.college != null)
                            .map(cc -> {
                                CollegeLink cl = new CollegeLink();
                                cl.collegeId = cc.college.getId();
                                cl.collegeName = cc.getCollege().getName();
                                cl.collegeCode = cc.getCollege().getCode();
                                cl.active = cc.isActive();
                                return cl;
                            })
                            .collect(Collectors.toList());

                    detail.totalCollegesLinked = collegeIds.size();

                    // Drives and stats
                    List<DriveInfo> allDrives = new ArrayList<>();
                    int totalOffers = 0;
                    int totalApplications = 0;
                    BigDecimal highestCtc = BigDecimal.ZERO;

                    for (CompanyCollege cc : managed) {
                        List<Drive> drives = DriveRepository.INSTANCE.byCompanyCollege(cc.getId());
                        for (Drive d : drives) {
                            DriveInfo di = new DriveInfo();
                            di.driveId = d.getId();
                            di.title = d.title;
                            di.status = d.status != null ? d.status.name() : null;
                            di.ctcOffered = d.ctcOffered;
                            di.employmentType = d.employmentType != null ? d.employmentType.name() : null;
                            di.driveDate = d.driveDate != null ? d.driveDate.toString() : null;
                            di.collegeName = cc.college != null ? cc.college.name : null;
                            if (cc.company != null) di.companyName = cc.company.name;

                            int appCount = d.applications != null ? d.applications.size() : 0;
                            int offCount = d.offers != null ? d.offers.size() : 0;
                            di.applicationCount = appCount;
                            di.offerCount = offCount;

                            totalApplications += appCount;
                            totalOffers += offCount;

                            if (d.ctcOffered != null && d.ctcOffered.compareTo(highestCtc) > 0) {
                                highestCtc = d.ctcOffered;
                            }

                            allDrives.add(di);
                        }
                    }

                    detail.totalDrives = allDrives.size();
                    detail.activeDrives = (int) allDrives.stream()
                            .filter(d -> d.status != null
                                    && !d.status.equals("COMPLETED")
                                    && !d.status.equals("CANCELLED"))
                            .count();
                    detail.totalOffers = totalOffers;
                    detail.totalApplications = totalApplications;
                    detail.highestCtcOffered = highestCtc;

                    // Recent drives (last 10)
                    detail.recentDrives = allDrives.stream().limit(10).collect(Collectors.toList());

                    return detail;
                })
                .subscribe(
                        o -> ResponseUtils.INSTANCE.writeJsonResponse(event, o),
                        error -> ResponseUtils.INSTANCE.handleError(event, error)
                );
    }

    @Data
    public static class CompanyHrDetail {
        Long id;
        String name;
        String email;
        String mobile;
        String userType;
        boolean verified;
        boolean active;
        String createdAt;
        Long collegeId;
        String collegeName;
        Long companyId;
        String companyName;

        // Companies managed
        List<CompanyInfo> companies;

        // Linked colleges
        List<CollegeLink> linkedColleges;
        int totalCollegesLinked;

        // Stats
        int totalDrives;
        int activeDrives;
        int totalOffers;
        int totalApplications;
        BigDecimal highestCtcOffered;

        // Recent drives
        List<DriveInfo> recentDrives;
    }

    @Data
    public static class CompanyInfo {
        Long companyId;
        String name;
        String industry;
        String website;
        String headquarters;
        boolean startup;
        String description;
    }

    @Data
    public static class CollegeLink {
        Long collegeId;
        String collegeName;
        String collegeCode;
        boolean active;
    }

    @Data
    public static class DriveInfo {
        Long driveId;
        String title;
        String companyName;
        String collegeName;
        String status;
        BigDecimal ctcOffered;
        String employmentType;
        String driveDate;
        int applicationCount;
        int offerCount;
    }
}

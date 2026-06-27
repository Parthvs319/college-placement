package superadmin.company;

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
                    detail.name = user.getName();
                    detail.email = user.getEmail();
                    detail.mobile = user.getMobile();
                    detail.userType = user.getUserType().getValue();
                    detail.verified = user.isVerified();
                    detail.active = user.isActive();
                    detail.createdAt = user.getCreatedAt() != null ? user.getCreatedAt().toString() : null;

                    if (user.getCollege() != null) {
                        detail.collegeId = user.getCollege().getId();
                        detail.collegeName = user.getCollege().getName();
                    }

                    // Direct company link
                    if (user.getCompany() != null) {
                        detail.companyId = user.getCompany().getId();
                        detail.companyName = user.getCompany().getName();
                    }

                    // Find company-college links managed by this HR
                    List<CompanyCollege> managed = CompanyCollegeRepository.INSTANCE.byManagedUser(userId);

                    // Companies
                    Map<Long, Company> companiesMap = new HashMap<>();
                    for (CompanyCollege cc : managed) {
                        if (cc.getCompany() != null) {
                            companiesMap.put(cc.getCompany().getId(), cc.getCompany());
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
                            .filter(cc -> cc.getCollege() != null)
                            .map(cc -> cc.getCollege().getId())
                            .collect(Collectors.toSet());

                    detail.linkedColleges = managed.stream()
                            .filter(cc -> cc.getCollege() != null)
                            .map(cc -> {
                                CollegeLink cl = new CollegeLink();
                                cl.collegeId = cc.getCollege().getId();
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
                            di.title = d.getTitle();
                            di.status = d.getStatus() != null ? d.getStatus().name() : null;
                            di.ctcOffered = d.getCtcOffered();
                            di.employmentType = d.getEmploymentType() != null ? d.getEmploymentType().name() : null;
                            di.driveDate = d.getDriveDate() != null ? d.getDriveDate().toString() : null;
                            di.collegeName = cc.getCollege() != null ? cc.getCollege().getName() : null;
                            if (cc.getCompany() != null) di.companyName = cc.getCompany().getName();

                            int appCount = d.getApplications() != null ? d.getApplications().size() : 0;
                            int offCount = d.getOffers() != null ? d.getOffers().size() : 0;
                            di.applicationCount = appCount;
                            di.offerCount = offCount;

                            totalApplications += appCount;
                            totalOffers += offCount;

                            if (d.getCtcOffered() != null && d.getCtcOffered().compareTo(highestCtc) > 0) {
                                highestCtc = d.getCtcOffered();
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

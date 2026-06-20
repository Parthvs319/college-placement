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
import java.util.List;
import java.util.stream.Collectors;

import lombok.Data;

/**
 * GET /admin/tpo/:userId
 * Returns detailed info about a TPO or College Admin user,
 * including their college stats, recent drives, and linked companies.
 */
@SuperAdminRole
public enum GetTpoDetailController implements BaseController {

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

                    TpoDetail detail = new TpoDetail();

                    detail.id = user.getId();
                    detail.name = user.getName();
                    detail.email = user.getEmail();
                    detail.mobile = user.getMobile();
                    detail.userType = user.getUserType().getValue();
                    detail.verified = user.isVerified();
                    detail.active = user.isActive();
                    detail.createdAt = user.getCreatedAt() != null ? user.getCreatedAt().toString() : null;

                    if (user.getCollege() != null) {
                        College c = user.getCollege();
                        detail.collegeId = c.getId();
                        detail.collegeName = c.getName();
                        detail.collegeCode = c.getCode();
                        detail.collegeUniversity = c.getUniversity();
                        detail.collegeWebsite = c.getWebsite();
                        detail.collegeContactEmail = c.getContactEmail();
                        detail.collegeContactPhone = c.getContactPhone();
                        detail.collegeVerified = c.isVerified();
                        detail.collegeActive = c.isActive();
                        detail.collegeDepartments = c.getDepartments();
                        Long collegeId = c.getId();
                        List<Student> students = StudentRepository.INSTANCE.byCollege(collegeId);
                        detail.totalStudents = students.size();
                        detail.placedStudents = (int) students.stream().filter(s -> s.isPlaced()).count();
                        detail.unplacedStudents = detail.totalStudents - detail.placedStudents;

                        List<CompanyCollege> ccList = CompanyCollegeRepository.INSTANCE.byCollege(collegeId);
                        detail.totalCompaniesLinked = ccList.size();

                        // Drive stats
                        List<Drive> drives = DriveRepository.INSTANCE.byCollege(collegeId);
                        detail.totalDrives = drives.size();
                        detail.activeDrives = (int) drives.stream()
                                .filter(d -> d.getStatus() != null
                                        && !d.getStatus().name().equals("COMPLETED")
                                        && !d.getStatus().name().equals("CANCELLED"))
                                .count();

                        int totalOffers = 0;
                        for (Drive d : drives) {
                            totalOffers += OfferRepository.INSTANCE.byDrive(d.getId()).size();
                        }
                        detail.totalOffers = totalOffers;

                        // Placement rate
                        if (detail.totalStudents > 0) {
                            detail.placementRate = Math.round((double) detail.placedStudents / detail.totalStudents * 100);
                        }

                        detail.recentDrives = drives.stream().limit(10).map(d -> {
                            DriveInfo di = new DriveInfo();
                            di.driveId = d.getId();
                            di.title = d.getTitle();
                            di.status = d.getStatus() != null ? d.getStatus().name() : null;
                            di.ctcOffered = d.getCtcOffered();
                            di.employmentType = d.getEmploymentType() != null ? d.getEmploymentType().name() : null;
                            di.driveDate = d.getDriveDate() != null ? d.getDriveDate().toString() : null;
                            if (d.getCompanyCollege() != null && d.getCompanyCollege().getCompany() != null) {
                                di.companyName = d.getCompanyCollege().getCompany().getName();
                            }
                            di.applicationCount = d.getApplications() != null ? d.getApplications().size() : 0;
                            di.offerCount = d.getOffers() != null ? d.getOffers().size() : 0;
                            return di;
                        }).collect(Collectors.toList());

                        // Linked companies
                        detail.linkedCompanies = ccList.stream().map(cc -> {
                            CompanyLink cl = new CompanyLink();
                            if (cc.getCompany() != null) {
                                cl.companyId = cc.getCompany().getId();
                                cl.companyName = cc.getCompany().getName();
                                cl.industry = cc.getCompany().getIndustry();
                                cl.startup = cc.getCompany().isStartup();
                            }
                            cl.active = cc.isActive();
                            List<Drive> compDrives = DriveRepository.INSTANCE.byCompanyCollege(cc.getId());
                            cl.driveCount = compDrives.size();
                            int offers = 0;
                            for (Drive d : compDrives) {
                                offers += OfferRepository.INSTANCE.byDrive(d.getId()).size();
                            }
                            cl.offerCount = offers;
                            return cl;
                        }).collect(Collectors.toList());
                    }

                    return detail;
                })
                .subscribe(
                        o -> ResponseUtils.INSTANCE.writeJsonResponse(event, o),
                        error -> ResponseUtils.INSTANCE.handleError(event, error)
                );
    }

    @Data
    public static class TpoDetail {
        Long id;
        String name;
        String email;
        String mobile;
        String userType;
        boolean verified;
        boolean active;
        String createdAt;

        // College
        Long collegeId;
        String collegeName;
        String collegeCode;
        String collegeUniversity;
        String collegeWebsite;
        String collegeContactEmail;
        String collegeContactPhone;
        boolean collegeVerified;
        boolean collegeActive;
        List<String> collegeDepartments;

        // Stats
        int totalStudents;
        int placedStudents;
        int unplacedStudents;
        int totalCompaniesLinked;
        int totalDrives;
        int activeDrives;
        int totalOffers;
        long placementRate;

        // Drives
        List<DriveInfo> recentDrives;

        // Companies
        List<CompanyLink> linkedCompanies;
    }

    @Data
    public static class DriveInfo {
        Long driveId;
        String title;
        String companyName;
        String status;
        BigDecimal ctcOffered;
        String employmentType;
        String driveDate;
        int applicationCount;
        int offerCount;
    }

    @Data
    public static class CompanyLink {
        Long companyId;
        String companyName;
        String industry;
        boolean startup;
        boolean active;
        int driveCount;
        int offerCount;
    }
}

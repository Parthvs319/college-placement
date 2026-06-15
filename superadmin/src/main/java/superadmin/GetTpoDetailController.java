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

                    // Basic info
                    detail.id = user.getId();
                    detail.name = user.name;
                    detail.email = user.email;
                    detail.mobile = user.mobile;
                    detail.userType = user.userType.getValue();
                    detail.verified = user.verified;
                    detail.active = user.active;
                    detail.createdAt = user.getCreatedAt() != null ? user.getCreatedAt().toString() : null;

                    // College info
                    if (user.college != null) {
                        College c = user.college;
                        detail.collegeId = c.getId();
                        detail.collegeName = c.name;
                        detail.collegeCode = c.code;
                        detail.collegeUniversity = c.university;
                        detail.collegeWebsite = c.website;
                        detail.collegeContactEmail = c.contactEmail;
                        detail.collegeContactPhone = c.contactPhone;
                        detail.collegeVerified = c.verified;
                        detail.collegeActive = c.active;
                        detail.collegeDepartments = c.departments;

                        Long collegeId = c.getId();

                        // Student stats
                        List<Student> students = StudentRepository.INSTANCE.byCollege(collegeId);
                        detail.totalStudents = students.size();
                        detail.placedStudents = (int) students.stream().filter(s -> s.placed).count();
                        detail.unplacedStudents = detail.totalStudents - detail.placedStudents;

                        // Company links
                        List<CompanyCollege> ccList = CompanyCollegeRepository.INSTANCE.byCollege(collegeId);
                        detail.totalCompaniesLinked = ccList.size();

                        // Drive stats
                        List<Drive> drives = DriveRepository.INSTANCE.byCollege(collegeId);
                        detail.totalDrives = drives.size();
                        detail.activeDrives = (int) drives.stream()
                                .filter(d -> d.status != null
                                        && !d.status.name().equals("COMPLETED")
                                        && !d.status.name().equals("CANCELLED"))
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

                        // Recent drives (last 10)
                        detail.recentDrives = drives.stream().limit(10).map(d -> {
                            DriveInfo di = new DriveInfo();
                            di.driveId = d.getId();
                            di.title = d.title;
                            di.status = d.status != null ? d.status.name() : null;
                            di.ctcOffered = d.ctcOffered;
                            di.employmentType = d.employmentType != null ? d.employmentType.name() : null;
                            di.driveDate = d.driveDate != null ? d.driveDate.toString() : null;
                            if (d.companyCollege != null && d.companyCollege.company != null) {
                                di.companyName = d.companyCollege.company.name;
                            }
                            di.applicationCount = d.applications != null ? d.applications.size() : 0;
                            di.offerCount = d.offers != null ? d.offers.size() : 0;
                            return di;
                        }).collect(Collectors.toList());

                        // Linked companies
                        detail.linkedCompanies = ccList.stream().map(cc -> {
                            CompanyLink cl = new CompanyLink();
                            if (cc.company != null) {
                                cl.companyId = cc.company.getId();
                                cl.companyName = cc.company.name;
                                cl.industry = cc.company.industry;
                                cl.startup = cc.company.startup;
                            }
                            cl.active = cc.active;
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

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
 * GET /admin/drives/:driveId
 * Returns detailed info about a drive including company, college,
 * rounds, applications, and offers.
 */
@SuperAdminRole
public enum GetDriveDetailController implements BaseController {

    INSTANCE;

    @Override
    public void handle(RoutingContext event) {
        SuperAdminAccessMiddleware.INSTANCE.with(event, new ArrayList<>(), this.getClass())
                .map(req -> {
                    Long driveId = Long.parseLong(event.pathParam("driveId"));
                    Drive drive = DriveRepository.INSTANCE.byId(driveId);
                    if (drive == null) throw new RoutingError(404, "Drive not found");

                    DriveDetail detail = new DriveDetail();

                    // Basic info
                    detail.id = drive.getId();
                    detail.title = drive.getTitle();
                    detail.jobDescription = drive.getJobDescription();
                    detail.status = drive.getStatus() != null ? drive.getStatus().name() : null;
                    detail.employmentType = drive.getEmploymentType() != null ? drive.getEmploymentType().name() : null;
                    detail.academicYear = drive.getAcademicYear();
                    detail.ctcOffered = drive.getCtcOffered();
                    detail.stipend = drive.getStipend();
                    detail.location = drive.getLocation();
                    detail.remote = drive.isRemote();
                    detail.venue = drive.getVenue();
                    detail.driveDate = drive.getDriveDate() != null ? drive.getDriveDate().toString() : null;
                    detail.registrationDeadline = drive.getRegistrationDeadline() != null ? drive.getRegistrationDeadline().toString() : null;

                    // Eligibility
                    detail.minCgpa = drive.getMinCgpa();
                    detail.maxActiveBacklogs = drive.getMaxActiveBacklogs();
                    detail.eligibleDepartments = drive.getEligibleDepartments();
                    detail.eligibleGenders = drive.getEligibleGenders();
                    detail.minPassingYear = drive.getMinPassingYear();
                    detail.maxPassingYear = drive.getMaxPassingYear();
                    detail.requiredSkills = drive.getRequiredSkills();
                    detail.niceToHaveSkills = drive.getNiceToHaveSkills();

                    // Company and College info via CompanyCollege
                    CompanyCollege cc = drive.getCompanyCollege();
                    if (cc != null) {
                        Company comp = cc.getCompany();
                        if (comp != null) {
                            detail.companyId = comp.getId();
                            detail.companyName = comp.getName();
                            detail.companyIndustry = comp.getIndustry();
                            detail.companyLogoUrl = comp.getLogoUrl();
                        }
                        College col = cc.getCollege();
                        if (col != null) {
                            detail.collegeId = col.getId();
                            detail.collegeName = col.getName();
                            detail.collegeCode = col.getCode();
                        }
                    }

                    // Rounds
                    List<DriveRound> rounds = drive.getRounds();
                    if (rounds != null) {
                        detail.rounds = rounds.stream().map(r -> {
                            RoundInfo ri = new RoundInfo();
                            ri.id = r.getId();
                            ri.roundNumber = r.getRoundNumber();
                            ri.roundType = r.getRoundType() != null ? r.getRoundType().name() : null;
                            ri.name = r.getName();
                            ri.description = r.getDescription();
                            ri.scheduledAt = r.getScheduledAt() != null ? r.getScheduledAt().toString() : null;
                            ri.durationMinutes = r.getDurationMinutes();
                            ri.venue = r.getVenue();
                            ri.completed = r.isCompleted();
                            return ri;
                        }).collect(Collectors.toList());
                    } else {
                        detail.rounds = new ArrayList<>();
                    }

                    // Applications
                    List<DriveApplication> applications = drive.getApplications();
                    if (applications != null) {
                        detail.totalApplications = applications.size();
                        detail.applications = applications.stream().map(a -> {
                            ApplicationInfo ai = new ApplicationInfo();
                            ai.id = a.getId();
                            ai.status = a.getStatus() != null ? a.getStatus().name() : null;
                            Student s = a.getStudent();
                            if (s != null) {
                                ai.studentId = s.getId();
                                User u = s.getUser();
                                if (u != null) {
                                    ai.studentName = u.getName();
                                    ai.studentEmail = u.getEmail();
                                }
                                ai.department = s.getDepartment();
                                ai.cgpa = s.getCgpa();
                            }
                            return ai;
                        }).collect(Collectors.toList());
                    } else {
                        detail.totalApplications = 0;
                        detail.applications = new ArrayList<>();
                    }

                    // Offers
                    List<Offer> offers = drive.getOffers();
                    if (offers != null) {
                        detail.totalOffers = offers.size();
                        detail.offers = offers.stream().map(o -> {
                            OfferInfo oi = new OfferInfo();
                            oi.id = o.getId();
                            oi.ctcOffered = o.getCtcOffered();
                            oi.designation = o.getDesignation();
                            oi.status = o.getStatus() != null ? o.getStatus().name() : null;
                            oi.offerDate = o.getCreatedAt() != null ? o.getCreatedAt().toString() : null;
                            Student s = o.getStudent();
                            if (s != null) {
                                oi.studentId = s.getId();
                                User u = s.getUser();
                                if (u != null) {
                                    oi.studentName = u.getName();
                                }
                                oi.department = s.getDepartment();
                            }
                            return oi;
                        }).collect(Collectors.toList());
                    } else {
                        detail.totalOffers = 0;
                        detail.offers = new ArrayList<>();
                    }

                    return detail;
                })
                .subscribe(
                        o -> ResponseUtils.INSTANCE.writeJsonResponse(event, o),
                        error -> ResponseUtils.INSTANCE.handleError(event, error)
                );
    }

    @Data
    public static class DriveDetail {
        Long id;
        String title;
        String jobDescription;
        String status;
        String employmentType;
        int academicYear;
        BigDecimal ctcOffered;
        BigDecimal stipend;
        String location;
        boolean remote;
        String venue;
        String driveDate;
        String registrationDeadline;

        // Eligibility
        BigDecimal minCgpa;
        int maxActiveBacklogs;
        List<String> eligibleDepartments;
        List<String> eligibleGenders;
        Integer minPassingYear;
        Integer maxPassingYear;
        List<String> requiredSkills;
        List<String> niceToHaveSkills;

        // Company/College
        Long companyId;
        String companyName;
        String companyIndustry;
        String companyLogoUrl;
        Long collegeId;
        String collegeName;
        String collegeCode;

        // Rounds
        List<RoundInfo> rounds;

        // Applications
        int totalApplications;
        List<ApplicationInfo> applications;

        // Offers
        int totalOffers;
        List<OfferInfo> offers;
    }

    @Data
    public static class RoundInfo {
        Long id;
        int roundNumber;
        String roundType;
        String name;
        String description;
        String scheduledAt;
        Integer durationMinutes;
        String venue;
        boolean completed;
    }

    @Data
    public static class ApplicationInfo {
        Long id;
        Long studentId;
        String studentName;
        String studentEmail;
        String department;
        BigDecimal cgpa;
        String status;
    }

    @Data
    public static class OfferInfo {
        Long id;
        Long studentId;
        String studentName;
        String department;
        String designation;
        BigDecimal ctcOffered;
        String status;
        String offerDate;
    }
}

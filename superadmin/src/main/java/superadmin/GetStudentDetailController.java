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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import lombok.Data;

@SuperAdminRole
public enum GetStudentDetailController implements BaseController {

    INSTANCE;

    @Override
    public void handle(RoutingContext event) {
        SuperAdminAccessMiddleware.INSTANCE.with(event, new ArrayList<>(), this.getClass())
                .map(req -> {
                    String idParam = event.request().getParam("studentId");
                    if (idParam == null) throw new RoutingError("studentId is required");
                    long studentId = Long.parseLong(idParam);

                    Student student = StudentRepository.INSTANCE.byId(studentId);
                    if (student == null) throw new RoutingError("Student not found");

                    StudentDetail detail = new StudentDetail();

                    // Basic info
                    detail.id = student.getId();
                    detail.name = student.getUser() != null ? student.getUser().getName() : null;
                    detail.email = student.getUser() != null ? student.getUser().getEmail() : null;
                    detail.mobile = student.getUser() != null ? student.getUser().getMobile() : null;
                    detail.verified = student.getUser() != null && student.getUser().isVerified();
                    detail.active = student.getUser() != null && student.getUser().isActive();

                    // Academic info
                    detail.enrollmentNumber = student.getEnrollmentNumber();
                    detail.department = student.getDepartment();
                    detail.passingYear = student.getPassingYear();
                    detail.cgpa = student.getCgpa();
                    detail.activeBacklogs = student.getActiveBacklogs();
                    detail.totalBacklogs = student.getTotalBacklogs();
                    detail.tenthPercentage = student.getTenthPercentage();
                    detail.twelfthPercentage = student.getTwelfthPercentage();
                    detail.diplomaPercentage = student.getDiplomaPercentage();
                    detail.gender = student.getGender();
                    detail.dateOfBirth = student.getDateOfBirth();

                    // Skills and links
                    detail.skills = student.getSkills();
                    detail.certifications = student.getCertifications();
                    detail.linkedinUrl = student.getLinkedinUrl();
                    detail.githubUrl = student.getGithubUrl();
                    detail.portfolioUrl = student.getPortfolioUrl();
                    detail.atsScore = student.getAtsScore();

                    // Placement status
                    detail.placed = student.isPlaced();
                    detail.placedAt = student.getPlacedAt() != null ? student.getPlacedAt().toString() : null;
                    detail.optedOut = student.isOptedOut();
                    detail.currentCtc = student.getCurrentCtc();

                    // College info
                    if (student.getCollege() != null) {
                        detail.collegeId = student.getCollege().getId();
                        detail.collegeName = student.getCollege().getName();
                        detail.collegeCode = student.getCollege().getCode();
                    }

                    // Drives applied (eager fetch drive -> companyCollege -> company)
                    List<DriveApplication> apps = DB.find(DriveApplication.class)
                            .fetch("drive")
                            .fetch("drive.companyCollege")
                            .fetch("drive.companyCollege.company")
                            .where()
                            .eq("student.id", studentId)
                            .eq("deleted", false)
                            .orderBy("createdAt desc")
                            .findList();
                    detail.applications = apps.stream().map(app -> {
                        ApplicationInfo ai = new ApplicationInfo();
                        ai.applicationId = app.getId();
                        ai.status = app.getStatus() != null ? app.getStatus().name() : null;
                        if (app.getDrive() != null) {
                            ai.driveId = app.getDrive().getId();
                            ai.driveTitle = app.getDrive().getTitle();
                            ai.ctcOffered = app.getDrive().getCtcOffered();
                            ai.driveStatus = app.getDrive().getStatus() != null ? app.getDrive().getStatus().name() : null;
                            if (app.getDrive().getCompanyCollege() != null && app.getDrive().getCompanyCollege().getCompany() != null) {
                                ai.companyId = app.getDrive().getCompanyCollege().getCompany().getId();
                                ai.companyName = app.getDrive().getCompanyCollege().getCompany().getName();
                            }
                        }
                        ai.appliedAt = app.getCreatedAt() != null ? app.getCreatedAt().toString() : null;
                        return ai;
                    }).collect(Collectors.toList());

                    // Offers (eager fetch drive -> companyCollege -> company)
                    List<Offer> offers = DB.find(Offer.class)
                            .fetch("drive")
                            .fetch("drive.companyCollege")
                            .fetch("drive.companyCollege.company")
                            .where()
                            .eq("student.id", studentId)
                            .eq("deleted", false)
                            .orderBy("createdAt desc")
                            .findList();
                    detail.offers = offers.stream().map(o -> {
                        OfferInfo oi = new OfferInfo();
                        oi.offerId = o.getId();
                        oi.ctcOffered = o.getCtcOffered();
                        oi.designation = o.getDesignation();
                        oi.location = o.getLocation();
                        oi.status = o.getStatus() != null ? o.getStatus().name() : null;
                        oi.offerLetterUrl = o.getOfferLetterUrl();
                        if (o.getDrive() != null) {
                            oi.driveId = o.getDrive().getId();
                            oi.driveTitle = o.getDrive().getTitle();
                            if (o.getDrive().getCompanyCollege() != null && o.getDrive().getCompanyCollege().getCompany() != null) {
                                oi.companyId = o.getDrive().getCompanyCollege().getCompany().getId();
                                oi.companyName = o.getDrive().getCompanyCollege().getCompany().getName();
                            }
                        }
                        oi.createdAt = o.getCreatedAt() != null ? o.getCreatedAt().toString() : null;
                        return oi;
                    }).collect(Collectors.toList());

                    // Subscriptions
                    List<Subscription> subs = SubscriptionRepository.INSTANCE.byStudent(studentId);
                    detail.subscriptions = subs.stream().map(sub -> {
                        SubscriptionInfo si = new SubscriptionInfo();
                        si.id = sub.getId();
                        si.tier = sub.getTier() != null ? sub.getTier().name() : "FREE";
                        si.active = sub.isActive();
                        si.startDate = sub.getStartDate() != null ? sub.getStartDate().toString() : null;
                        si.endDate = sub.getEndDate() != null ? sub.getEndDate().toString() : null;
                        si.totalCredits = sub.getTotalCredits();
                        si.usedCredits = sub.getUsedCredits();
                        return si;
                    }).collect(Collectors.toList());

                    // Keep top-level fields for backward compat
                    if (!subs.isEmpty()) {
                        Subscription sub = subs.get(0);
                        detail.subscriptionTier = sub.getTier() != null ? sub.getTier().name() : "FREE";
                        detail.subscriptionActive = sub.isActive();
                        detail.subscriptionStartDate = sub.getStartDate() != null ? sub.getStartDate().toString() : null;
                        detail.subscriptionEndDate = sub.getEndDate() != null ? sub.getEndDate().toString() : null;
                        detail.totalCredits = sub.getTotalCredits();
                        detail.usedCredits = sub.getUsedCredits();
                    } else {
                        detail.subscriptionTier = "FREE";
                        detail.subscriptionActive = false;
                    }

                    // Resumes
                    if (student.getResumes() != null) {
                        detail.resumeCount = student.getResumes().size();
                    }

                    return detail;
                })
                .subscribe(
                        o -> ResponseUtils.INSTANCE.writeJsonResponse(event, o),
                        error -> ResponseUtils.INSTANCE.handleError(event, error)
                );
    }

    @Data
    public static class StudentDetail {
        Long id;
        String name;
        String email;
        String mobile;
        boolean verified;
        boolean active;

        // Academic
        String enrollmentNumber;
        String department;
        int passingYear;
        BigDecimal cgpa;
        int activeBacklogs;
        int totalBacklogs;
        String tenthPercentage;
        String twelfthPercentage;
        String diplomaPercentage;
        String gender;
        String dateOfBirth;

        // Skills
        List<String> skills;
        List<String> certifications;
        String linkedinUrl;
        String githubUrl;
        String portfolioUrl;
        Integer atsScore;

        // Placement
        boolean placed;
        String placedAt;
        boolean optedOut;
        BigDecimal currentCtc;

        // College
        Long collegeId;
        String collegeName;
        String collegeCode;

        // Applications and Offers
        List<ApplicationInfo> applications;
        List<OfferInfo> offers;

        // Subscriptions
        List<SubscriptionInfo> subscriptions;
        String subscriptionTier;
        boolean subscriptionActive;
        String subscriptionStartDate;
        String subscriptionEndDate;
        int totalCredits;
        int usedCredits;

        // Resume
        int resumeCount;
    }

    @Data
    public static class ApplicationInfo {
        Long applicationId;
        Long driveId;
        Long companyId;
        String driveTitle;
        String companyName;
        BigDecimal ctcOffered;
        String status;
        String driveStatus;
        String appliedAt;
    }

    @Data
    public static class OfferInfo {
        Long offerId;
        Long driveId;
        Long companyId;
        String companyName;
        String driveTitle;
        BigDecimal ctcOffered;
        String designation;
        String location;
        String status;
        String offerLetterUrl;
        String createdAt;
    }

    @Data
    public static class SubscriptionInfo {
        Long id;
        String tier;
        boolean active;
        String startDate;
        String endDate;
        int totalCredits;
        int usedCredits;
    }
}

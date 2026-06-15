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
                    detail.name = student.user != null ? student.user.name : null;
                    detail.email = student.user != null ? student.user.email : null;
                    detail.mobile = student.user != null ? student.user.mobile : null;
                    detail.verified = student.user != null && student.user.verified;
                    detail.active = student.user != null && student.user.active;

                    // Academic info
                    detail.enrollmentNumber = student.enrollmentNumber;
                    detail.department = student.department;
                    detail.passingYear = student.passingYear;
                    detail.cgpa = student.cgpa;
                    detail.activeBacklogs = student.activeBacklogs;
                    detail.totalBacklogs = student.totalBacklogs;
                    detail.tenthPercentage = student.tenthPercentage;
                    detail.twelfthPercentage = student.twelfthPercentage;
                    detail.diplomaPercentage = student.diplomaPercentage;
                    detail.gender = student.gender;
                    detail.dateOfBirth = student.dateOfBirth;

                    // Skills and links
                    detail.skills = student.skills;
                    detail.certifications = student.certifications;
                    detail.linkedinUrl = student.linkedinUrl;
                    detail.githubUrl = student.githubUrl;
                    detail.portfolioUrl = student.portfolioUrl;
                    detail.atsScore = student.atsScore;

                    // Placement status
                    detail.placed = student.placed;
                    detail.optedOut = student.optedOut;
                    detail.currentCtc = student.currentCtc;

                    // College info
                    if (student.college != null) {
                        detail.collegeId = student.college.getId();
                        detail.collegeName = student.college.name;
                        detail.collegeCode = student.college.code;
                    }

                    // Drives applied
                    List<DriveApplication> apps = DriveApplicationRepository.INSTANCE.byStudent(studentId);
                    detail.applications = apps.stream().map(app -> {
                        ApplicationInfo ai = new ApplicationInfo();
                        ai.applicationId = app.getId();
                        ai.status = app.status != null ? app.status.name() : null;
                        if (app.drive != null) {
                            ai.driveId = app.drive.getId();
                            ai.driveTitle = app.drive.title;
                            ai.ctcOffered = app.drive.ctcOffered;
                            ai.driveStatus = app.drive.status != null ? app.drive.status.name() : null;
                            if (app.drive.companyCollege != null && app.drive.companyCollege.company != null) {
                                ai.companyName = app.drive.companyCollege.company.name;
                            }
                        }
                        ai.appliedAt = app.getCreatedAt() != null ? app.getCreatedAt().toString() : null;
                        return ai;
                    }).collect(Collectors.toList());

                    // Offers
                    List<Offer> offers = OfferRepository.INSTANCE.byStudent(studentId);
                    detail.offers = offers.stream().map(o -> {
                        OfferInfo oi = new OfferInfo();
                        oi.offerId = o.getId();
                        oi.ctcOffered = o.ctcOffered;
                        oi.designation = o.designation;
                        oi.location = o.location;
                        oi.status = o.status != null ? o.status.name() : null;
                        oi.offerLetterUrl = o.offerLetterUrl;
                        if (o.drive != null) {
                            oi.driveTitle = o.drive.title;
                            if (o.drive.companyCollege != null && o.drive.companyCollege.company != null) {
                                oi.companyName = o.drive.companyCollege.company.name;
                            }
                        }
                        oi.createdAt = o.getCreatedAt() != null ? o.getCreatedAt().toString() : null;
                        return oi;
                    }).collect(Collectors.toList());

                    // Subscription
                    List<Subscription> subs = SubscriptionRepository.INSTANCE.byStudent(studentId);
                    if (!subs.isEmpty()) {
                        Subscription sub = subs.get(0);
                        detail.subscriptionTier = sub.tier != null ? sub.tier.name() : "FREE";
                        detail.subscriptionActive = sub.active;
                        detail.subscriptionStartDate = sub.startDate != null ? sub.startDate.toString() : null;
                        detail.subscriptionEndDate = sub.endDate != null ? sub.endDate.toString() : null;
                        detail.totalCredits = sub.totalCredits;
                        detail.usedCredits = sub.usedCredits;
                    } else {
                        detail.subscriptionTier = "FREE";
                        detail.subscriptionActive = false;
                    }

                    // Resumes
                    if (student.resumes != null) {
                        detail.resumeCount = student.resumes.size();
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
        boolean optedOut;
        BigDecimal currentCtc;

        // College
        Long collegeId;
        String collegeName;
        String collegeCode;

        // Applications and Offers
        List<ApplicationInfo> applications;
        List<OfferInfo> offers;

        // Subscription
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
        String companyName;
        String driveTitle;
        BigDecimal ctcOffered;
        String designation;
        String location;
        String status;
        String offerLetterUrl;
        String createdAt;
    }
}

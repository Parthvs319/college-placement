package models.json;

import io.ebean.DB;
import lombok.Data;
import models.sql.*;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;


public final class CollegeDtos {

    private CollegeDtos() {}

    @Data
    public static class DriveResponse {
        Long id;
        String title;
        String jobDescription;
        String employmentType;
        String status;
        int academicYear;
        BigDecimal minCgpa;
        int maxActiveBacklogs;
        List<String> eligibleDepartments;
        Integer minPassingYear;
        Integer maxPassingYear;
        BigDecimal ctcOffered;
        BigDecimal stipend;
        String location;
        boolean isRemote;
        Timestamp registrationDeadline;
        Timestamp driveDate;
        String venue;
        String companyName;
        Long companyId;
        Timestamp createdAt;
    }

    public static DriveResponse toDriveDto(Drive d) {
        DriveResponse dto = new DriveResponse();
        dto.id = d.getId();
        dto.title = d.title;
        dto.jobDescription = d.jobDescription;
        dto.employmentType = d.employmentType != null ? d.employmentType.name() : null;
        dto.status = d.status != null ? d.status.name() : null;
        dto.academicYear = d.academicYear;
        dto.minCgpa = d.minCgpa;
        dto.maxActiveBacklogs = d.maxActiveBacklogs;
        dto.eligibleDepartments = d.eligibleDepartments;
        dto.minPassingYear = d.minPassingYear;
        dto.maxPassingYear = d.maxPassingYear;
        dto.ctcOffered = d.ctcOffered;
        dto.stipend = d.stipend;
        dto.location = d.location;
        dto.isRemote = d.isRemote;
        dto.registrationDeadline = d.registrationDeadline;
        dto.driveDate = d.driveDate;
        dto.venue = d.venue;
        dto.createdAt = d.getCreatedAt();
        if (d.companyCollege != null && d.companyCollege.company != null) {
            dto.companyName = d.companyCollege.company.name;
            dto.companyId = d.companyCollege.company.getId();
        }
        return dto;
    }

    // ── Drive Application ──

    @Data
    public static class ApplicationResponse {
        Long id;
        Long studentId;
        String studentName;
        String enrollmentNumber;
        String department;
        String status;
        String resumeSnapshot;
        String notes;
        Timestamp appliedAt;
    }

    public static ApplicationResponse toApplicationDto(DriveApplication a) {
        ApplicationResponse dto = new ApplicationResponse();
        dto.id = a.getId();
        dto.status = a.status != null ? a.status.name() : null;
        dto.resumeSnapshot = a.resumeSnapshot;
        dto.notes = a.notes;
        dto.appliedAt = a.getCreatedAt();
        if (a.student != null) {
            dto.studentId = a.student.getId();
            dto.enrollmentNumber = a.student.enrollmentNumber;
            dto.department = a.student.department;
            if (a.student.user != null) {
                dto.studentName = a.student.user.name;
            }
        }
        return dto;
    }

    // ── Round ──

    @Data
    public static class RoundResponse {
        Long id;
        int roundNumber;
        String roundType;
        String name;
        String description;
        Timestamp scheduledAt;
        Integer durationMinutes;
        String venue;
        boolean completed;
    }

    public static RoundResponse toRoundDto(DriveRound r) {
        RoundResponse dto = new RoundResponse();
        dto.id = r.getId();
        dto.roundNumber = r.roundNumber;
        dto.roundType = r.roundType != null ? r.roundType.name() : null;
        dto.name = r.name;
        dto.description = r.description;
        dto.scheduledAt = r.scheduledAt;
        dto.durationMinutes = r.durationMinutes;
        dto.venue = r.venue;
        dto.completed = r.completed;
        return dto;
    }

    // ── Round Result ──

    @Data
    public static class RoundResultResponse {
        Long id;
        Long roundId;
        Long studentId;
        String studentName;
        String enrollmentNumber;
        String status;
        BigDecimal score;
        String feedback;
        String interviewerName;
    }

    public static RoundResultResponse toRoundResultDto(RoundResult r) {
        RoundResultResponse dto = new RoundResultResponse();
        dto.id = r.getId();
        dto.roundId = r.round != null ? r.round.getId() : null;
        dto.status = r.status != null ? r.status.name() : null;
        dto.score = r.score;
        dto.feedback = r.feedback;
        dto.interviewerName = r.interviewerName;
        if (r.student != null) {
            dto.studentId = r.student.getId();
            dto.enrollmentNumber = r.student.enrollmentNumber;
            if (r.student.user != null) {
                dto.studentName = r.student.user.name;
            }
        }
        return dto;
    }

    // ── Offer ──

    @Data
    public static class OfferResponse {
        Long id;
        Long studentId;
        String studentName;
        String enrollmentNumber;
        Long driveId;
        String driveTitle;
        String companyName;
        BigDecimal ctcOffered;
        String designation;
        String location;
        String status;
        Timestamp responseDeadline;
        Timestamp respondedAt;
        String offerLetterUrl;
        String notes;
        Timestamp createdAt;
    }

    public static OfferResponse toOfferDto(Offer o) {
        OfferResponse dto = new OfferResponse();
        dto.id = o.getId();
        dto.ctcOffered = o.ctcOffered;
        dto.designation = o.designation;
        dto.location = o.location;
        dto.status = o.status != null ? o.status.name() : null;
        dto.responseDeadline = o.responseDeadline;
        dto.respondedAt = o.respondedAt;
        dto.offerLetterUrl = o.offerLetterUrl;
        dto.notes = o.notes;
        dto.createdAt = o.getCreatedAt();
        if (o.student != null) {
            dto.studentId = o.student.getId();
            dto.enrollmentNumber = o.student.enrollmentNumber;
            if (o.student.user != null) {
                dto.studentName = o.student.user.name;
            }
        }
        if (o.drive != null) {
            dto.driveId = o.drive.getId();
            dto.driveTitle = o.drive.title;
            if (o.drive.companyCollege != null && o.drive.companyCollege.company != null) {
                dto.companyName = o.drive.companyCollege.company.name;
            }
        }
        return dto;
    }

    // ── Company-College Link ──

    @Data
    public static class CompanyCollegeResponse {
        Long id;
        Long companyId;
        String companyName;
        String industry;
        String logoUrl;
        boolean companyCanManage;
        boolean active;
    }

    public static CompanyCollegeResponse toCompanyCollegeDto(CompanyCollege cc) {
        CompanyCollegeResponse dto = new CompanyCollegeResponse();
        dto.id = cc.getId();
        dto.companyCanManage = cc.companyCanManage;
        dto.active = cc.active;
        if (cc.company != null) {
            dto.companyId = cc.company.getId();
            dto.companyName = cc.company.name;
            dto.industry = cc.company.industry;
            dto.logoUrl = cc.company.logoUrl;
        }
        return dto;
    }

    // ── College ──

    @Data
    public static class CollegeResponse {
        Long id;
        String name;
        String code;
        String university;
        String address;
        Long cityId;
        Long stateId;
        String city;
        String state;
        String website;
        String logoUrl;
        String contactEmail;
        String contactPhone;
        List<String> departments;
        boolean verified;
        boolean active;
    }

    public static CollegeResponse toCollegeDto(College c) {
        CollegeResponse dto = new CollegeResponse();
        dto.id = c.getId();
        dto.name = c.name;
        dto.code = c.code;
        dto.university = c.university;
        dto.address = c.address;
        dto.cityId = c.cityId;
        dto.stateId = c.stateId;
        if (c.cityId != null) {
            City ct = DB.find(City.class, c.cityId);
            if (ct != null) dto.city = ct.name;
        }
        if (c.stateId != null) {
            States st = DB.find(States.class, c.stateId);
            if (st != null) dto.state = st.name;
        }
        dto.website = c.website;
        dto.logoUrl = c.logoUrl;
        dto.contactEmail = c.contactEmail;
        dto.contactPhone = c.contactPhone;
        dto.departments = c.departments;
        dto.verified = c.verified;
        dto.active = c.active;
        return dto;
    }

    // ── Placement Policy ──

    @Data
    public static class PolicyResponse {
        Long id;
        int academicYear;
        BigDecimal dreamCtcThreshold;
        int maxSimultaneousOffers;
        boolean blockAfterFirstAccept;
        boolean autoFilterEnabled;
        int offerExpiryDays;
        String description;
    }

    public static PolicyResponse toPolicyDto(PlacementPolicy p) {
        PolicyResponse dto = new PolicyResponse();
        dto.id = p.getId();
        dto.academicYear = p.academicYear;
        dto.dreamCtcThreshold = p.dreamCtcThreshold;
        dto.maxSimultaneousOffers = p.maxSimultaneousOffers;
        dto.blockAfterFirstAccept = p.blockAfterFirstAccept;
        dto.autoFilterEnabled = p.autoFilterEnabled;
        dto.offerExpiryDays = p.offerExpiryDays;
        dto.description = p.description;
        return dto;
    }

    // ── Document ──

    @Data
    public static class DocumentResponse {
        Long id;
        String name;
        String type;
        String fileUrl;
        String fileType;
        Long fileSizeBytes;
        int academicYear;
        String uploadedBy;
        Timestamp createdAt;
    }

    public static DocumentResponse toDocumentDto(Document d) {
        DocumentResponse dto = new DocumentResponse();
        dto.id = d.getId();
        dto.name = d.name;
        dto.type = d.type;
        dto.fileUrl = d.fileUrl;
        dto.fileType = d.fileType;
        dto.fileSizeBytes = d.fileSizeBytes;
        dto.academicYear = d.academicYear;
        dto.createdAt = d.getCreatedAt();
        if (d.uploadedByUser != null) {
            dto.uploadedBy = d.uploadedByUser.name;
        }
        return dto;
    }

    // ── Notification ──

    @Data
    public static class NotificationResponse {
        Long id;
        String channel;
        String type;
        String subject;
        String body;
        int recipientCount;
        int deliveredCount;
        int failedCount;
        Timestamp sentAt;
        Map<String, String> metadata;
        Long driveId;
        String driveTitle;
        Timestamp createdAt;
    }

    public static NotificationResponse toNotificationDto(Notification n) {
        NotificationResponse dto = new NotificationResponse();
        dto.id = n.getId();
        dto.channel = n.channel;
        dto.type = n.type;
        dto.subject = n.subject;
        dto.body = n.body;
        dto.recipientCount = n.recipientCount;
        dto.deliveredCount = n.deliveredCount;
        dto.failedCount = n.failedCount;
        dto.sentAt = n.sentAt;
        dto.metadata = n.metadata;
        dto.createdAt = n.getCreatedAt();
        if (n.drive != null) {
            dto.driveId = n.drive.getId();
            dto.driveTitle = n.drive.title;
        }
        return dto;
    }
}

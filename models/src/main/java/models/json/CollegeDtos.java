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
        String driveCode;
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
        dto.driveCode = d.getDriveCode();
        dto.title = d.getTitle();
        dto.jobDescription = d.getJobDescription();
        dto.employmentType = d.getEmploymentType() != null ? d.getEmploymentType().name() : null;
        dto.status = d.getStatus() != null ? d.getStatus().name() : null;
        dto.academicYear = d.getAcademicYear();
        dto.minCgpa = d.getMinCgpa();
        dto.maxActiveBacklogs = d.getMaxActiveBacklogs();
        dto.eligibleDepartments = d.getEligibleDepartments();
        dto.minPassingYear = d.getMinPassingYear();
        dto.maxPassingYear = d.getMaxPassingYear();
        dto.ctcOffered = d.getCtcOffered();
        dto.stipend = d.getStipend();
        dto.location = d.getLocation();
        dto.isRemote = d.isRemote();
        dto.registrationDeadline = d.getRegistrationDeadline();
        dto.driveDate = d.getDriveDate();
        dto.venue = d.getVenue();
        dto.createdAt = d.getCreatedAt();
        CompanyCollege cc = d.getCompanyCollege();
        if (cc != null && cc.getCompany() != null) {
            dto.companyName = cc.getCompany().getName();
            dto.companyId = cc.getCompany().getId();
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
        dto.status = a.getStatus() != null ? a.getStatus().name() : null;
        dto.resumeSnapshot = a.getResumeSnapshot();
        dto.notes = a.getNotes();
        dto.appliedAt = a.getCreatedAt();
        Student student = a.getStudent();
        if (student != null) {
            dto.studentId = student.getId();
            dto.enrollmentNumber = student.getEnrollmentNumber();
            dto.department = student.getDepartment();
            User user = student.getUser();
            if (user != null) {
                dto.studentName = user.getName();
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
        dto.roundNumber = r.getRoundNumber();
        dto.roundType = r.getRoundType() != null ? r.getRoundType().name() : null;
        dto.name = r.getName();
        dto.description = r.getDescription();
        dto.scheduledAt = r.getScheduledAt();
        dto.durationMinutes = r.getDurationMinutes();
        dto.venue = r.getVenue();
        dto.completed = r.isCompleted();
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
        dto.roundId = r.getRound() != null ? r.getRound().getId() : null;
        dto.status = r.getStatus() != null ? r.getStatus().name() : null;
        dto.score = r.getScore();
        dto.feedback = r.getFeedback();
        dto.interviewerName = r.getInterviewerName();
        Student student = r.getStudent();
        if (student != null) {
            dto.studentId = student.getId();
            dto.enrollmentNumber = student.getEnrollmentNumber();
            User user = student.getUser();
            if (user != null) {
                dto.studentName = user.getName();
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
        dto.ctcOffered = o.getCtcOffered();
        dto.designation = o.getDesignation();
        dto.location = o.getLocation();
        dto.status = o.getStatus() != null ? o.getStatus().name() : null;
        dto.responseDeadline = o.getResponseDeadline();
        dto.respondedAt = o.getRespondedAt();
        dto.offerLetterUrl = o.getOfferLetterUrl();
        dto.notes = o.getNotes();
        dto.createdAt = o.getCreatedAt();
        Student student = o.getStudent();
        if (student != null) {
            dto.studentId = student.getId();
            dto.enrollmentNumber = student.getEnrollmentNumber();
            User user = student.getUser();
            if (user != null) {
                dto.studentName = user.getName();
            }
        }
        Drive drive = o.getDrive();
        if (drive != null) {
            dto.driveId = drive.getId();
            dto.driveTitle = drive.getTitle();
            CompanyCollege cc = drive.getCompanyCollege();
            if (cc != null && cc.getCompany() != null) {
                dto.companyName = cc.getCompany().getName();
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
        String companyCode;
        String industry;
        String logoUrl;
        boolean companyCanManage;
        boolean active;
    }

    public static CompanyCollegeResponse toCompanyCollegeDto(CompanyCollege cc) {
        CompanyCollegeResponse dto = new CompanyCollegeResponse();
        dto.id = cc.getId();
        dto.companyCanManage = cc.isCompanyCanManage();
        dto.active = cc.isActive();
        Company company = cc.getCompany();
        if (company != null) {
            dto.companyId = company.getId();
            dto.companyName = company.getName();
            dto.companyCode = company.getCode();
            dto.industry = company.getIndustry();
            dto.logoUrl = company.getLogoUrl();
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
        String tpoName;
        String gstin;
        boolean isEmailVerified;
        boolean isPhoneVerified;
        List<String> departments;
        boolean verified;
        boolean active;
    }

    public static CollegeResponse toCollegeDto(College c) {
        CollegeResponse dto = new CollegeResponse();
        dto.id = c.getId();
        dto.name = c.getName();
        dto.code = c.getCode();
        dto.university = c.getUniversity();
        dto.address = c.getAddress();
        dto.cityId = c.getCityId();
        dto.stateId = c.getStateId();
        if (c.getCityId() != null) {
            City ct = DB.find(City.class, c.getCityId());
            if (ct != null) dto.city = ct.getName();
        }
        if (c.getStateId() != null) {
            States st = DB.find(States.class, c.getStateId());
            if (st != null) dto.state = st.getName();
        }
        dto.website = c.getWebsite();
        dto.logoUrl = c.getLogoUrl();
        dto.contactEmail = c.getContactEmail();
        dto.contactPhone = c.getContactPhone();
        dto.tpoName = c.getTpoName();
        dto.gstin = c.getGstin();
        dto.isEmailVerified = c.isEmailVerified();
        dto.isPhoneVerified = c.isPhoneVerified();
        dto.departments = c.getDepartments();
        dto.verified = c.isVerified();
        dto.active = c.isActive();
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
        dto.academicYear = p.getAcademicYear();
        dto.dreamCtcThreshold = p.getDreamCtcThreshold();
        dto.maxSimultaneousOffers = p.getMaxSimultaneousOffers();
        dto.blockAfterFirstAccept = p.isBlockAfterFirstAccept();
        dto.autoFilterEnabled = p.isAutoFilterEnabled();
        dto.offerExpiryDays = p.getOfferExpiryDays();
        dto.description = p.getDescription();
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
        dto.name = d.getName();
        dto.type = d.getType();
        dto.fileUrl = d.getFileUrl();
        dto.fileType = d.getFileType();
        dto.fileSizeBytes = d.getFileSizeBytes();
        dto.academicYear = d.getAcademicYear();
        dto.createdAt = d.getCreatedAt();
        User uploader = d.getUploadedByUser();
        if (uploader != null) {
            dto.uploadedBy = uploader.getName();
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
        dto.channel = n.getChannel();
        dto.type = n.getType();
        dto.subject = n.getSubject();
        dto.body = n.getBody();
        dto.recipientCount = n.getRecipientCount();
        dto.deliveredCount = n.getDeliveredCount();
        dto.failedCount = n.getFailedCount();
        dto.sentAt = n.getSentAt();
        dto.metadata = n.getMetadata();
        dto.createdAt = n.getCreatedAt();
        Drive drive = n.getDrive();
        if (drive != null) {
            dto.driveId = drive.getId();
            dto.driveTitle = drive.getTitle();
        }
        return dto;
    }
}

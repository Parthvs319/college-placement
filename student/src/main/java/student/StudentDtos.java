package student;

import lombok.Data;
import models.sql.*;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

/**
 * Shared DTOs for all Student portal responses.
 */
public final class StudentDtos {

    private StudentDtos() {}

    // ── Student Profile ──

    @Data
    public static class ProfileResponse {
        Long id;
        String name;
        String email;
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
        List<String> skills;
        List<String> certifications;
        String linkedinUrl;
        String githubUrl;
        String portfolioUrl;
        String resumeUrl;
        boolean placed;
        boolean optedOut;
        boolean verified;
        BigDecimal currentCtc;
        String collegeName;
    }

    public static ProfileResponse toProfileDto(Student s) {
        ProfileResponse dto = new ProfileResponse();
        dto.id = s.getId();
        dto.enrollmentNumber = s.enrollmentNumber;
        dto.department = s.department;
        dto.passingYear = s.passingYear;
        dto.cgpa = s.cgpa;
        dto.activeBacklogs = s.activeBacklogs;
        dto.totalBacklogs = s.totalBacklogs;
        dto.tenthPercentage = s.tenthPercentage;
        dto.twelfthPercentage = s.twelfthPercentage;
        dto.diplomaPercentage = s.diplomaPercentage;
        dto.gender = s.gender;
        dto.dateOfBirth = s.dateOfBirth;
        dto.skills = s.skills;
        dto.certifications = s.certifications;
        dto.linkedinUrl = s.linkedinUrl;
        dto.githubUrl = s.githubUrl;
        dto.portfolioUrl = s.portfolioUrl;
        dto.resumeUrl = s.resumeUrl;
        dto.placed = s.placed;
        dto.optedOut = s.optedOut;
        dto.currentCtc = s.currentCtc;
        if (s.user != null) {
            dto.name = s.user.name;
            dto.email = s.user.email;
            dto.verified = s.user.verified;
        }
        if (s.college != null) {
            dto.collegeName = s.college.name;
        }
        return dto;
    }

    // ── Drive (student view — less detail than TPO view) ──

    @Data
    public static class DriveListItem {
        Long id;
        String title;
        String companyName;
        String companyLogoUrl;
        String employmentType;
        String status;
        BigDecimal ctcOffered;
        BigDecimal stipend;
        String location;
        boolean isRemote;
        BigDecimal minCgpa;
        int maxActiveBacklogs;
        List<String> eligibleDepartments;
        Integer minPassingYear;
        Integer maxPassingYear;
        Timestamp registrationDeadline;
        Timestamp driveDate;
        String venue;
    }

    public static DriveListItem toDriveListDto(Drive d) {
        DriveListItem dto = new DriveListItem();
        dto.id = d.getId();
        dto.title = d.title;
        dto.employmentType = d.employmentType != null ? d.employmentType.name() : null;
        dto.status = d.status != null ? d.status.name() : null;
        dto.ctcOffered = d.ctcOffered;
        dto.stipend = d.stipend;
        dto.location = d.location;
        dto.isRemote = d.isRemote;
        dto.minCgpa = d.minCgpa;
        dto.maxActiveBacklogs = d.maxActiveBacklogs;
        dto.eligibleDepartments = d.eligibleDepartments;
        dto.minPassingYear = d.minPassingYear;
        dto.maxPassingYear = d.maxPassingYear;
        dto.registrationDeadline = d.registrationDeadline;
        dto.driveDate = d.driveDate;
        dto.venue = d.venue;
        if (d.companyCollege != null && d.companyCollege.company != null) {
            dto.companyName = d.companyCollege.company.name;
            dto.companyLogoUrl = d.companyCollege.company.logoUrl;
        }
        return dto;
    }

    // ── Drive Detail (full info for a single drive) ──

    @Data
    public static class DriveDetailResponse {
        Long id;
        String title;
        String jobDescription;
        String companyName;
        String companyLogoUrl;
        String companyWebsite;
        String employmentType;
        String status;
        BigDecimal ctcOffered;
        BigDecimal stipend;
        String location;
        boolean isRemote;
        BigDecimal minCgpa;
        int maxActiveBacklogs;
        List<String> eligibleDepartments;
        Integer minPassingYear;
        Integer maxPassingYear;
        List<String> requiredSkills;
        List<String> niceToHaveSkills;
        Timestamp registrationDeadline;
        Timestamp driveDate;
        String venue;
    }

    public static DriveDetailResponse toDriveDetailDto(Drive d) {
        DriveDetailResponse dto = new DriveDetailResponse();
        dto.id = d.getId();
        dto.title = d.title;
        dto.jobDescription = d.jobDescription;
        dto.employmentType = d.employmentType != null ? d.employmentType.name() : null;
        dto.status = d.status != null ? d.status.name() : null;
        dto.ctcOffered = d.ctcOffered;
        dto.stipend = d.stipend;
        dto.location = d.location;
        dto.isRemote = d.isRemote;
        dto.minCgpa = d.minCgpa;
        dto.maxActiveBacklogs = d.maxActiveBacklogs;
        dto.eligibleDepartments = d.eligibleDepartments;
        dto.minPassingYear = d.minPassingYear;
        dto.maxPassingYear = d.maxPassingYear;
        dto.requiredSkills = d.requiredSkills;
        dto.niceToHaveSkills = d.niceToHaveSkills;
        dto.registrationDeadline = d.registrationDeadline;
        dto.driveDate = d.driveDate;
        dto.venue = d.venue;
        if (d.companyCollege != null && d.companyCollege.company != null) {
            dto.companyName = d.companyCollege.company.name;
            dto.companyLogoUrl = d.companyCollege.company.logoUrl;
            dto.companyWebsite = d.companyCollege.company.website;
        }
        return dto;
    }

    // ── Application (student's own view) ──

    @Data
    public static class MyApplicationResponse {
        Long id;
        Long driveId;
        String driveTitle;
        String companyName;
        String status;
        String resumeSnapshot;
        String notes;
        Timestamp appliedAt;
        Timestamp driveDate;
    }

    public static MyApplicationResponse toApplicationDto(DriveApplication a) {
        MyApplicationResponse dto = new MyApplicationResponse();
        dto.id = a.getId();
        dto.status = a.status != null ? a.status.name() : null;
        dto.resumeSnapshot = a.resumeSnapshot;
        dto.notes = a.notes;
        dto.appliedAt = a.getCreatedAt();
        if (a.drive != null) {
            dto.driveId = a.drive.getId();
            dto.driveTitle = a.drive.title;
            dto.driveDate = a.drive.driveDate;
            if (a.drive.companyCollege != null && a.drive.companyCollege.company != null) {
                dto.companyName = a.drive.companyCollege.company.name;
            }
        }
        return dto;
    }

    // ── Offer (student's own view) ──

    @Data
    public static class MyOfferResponse {
        Long id;
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

    public static MyOfferResponse toOfferDto(Offer o) {
        MyOfferResponse dto = new MyOfferResponse();
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
        if (o.drive != null) {
            dto.driveId = o.drive.getId();
            dto.driveTitle = o.drive.title;
            if (o.drive.companyCollege != null && o.drive.companyCollege.company != null) {
                dto.companyName = o.drive.companyCollege.company.name;
            }
        }
        return dto;
    }

    // ── PYQ ──

    @Data
    public static class PYQResponse {
        Long id;
        String companyName;
        String role;
        String roundType;
        int year;
        String content;
        String difficulty;
        List<String> tags;
        int upvotes;
        boolean anonymous;
        String contributedBy;
    }

    public static PYQResponse toPyqDto(PYQ p) {
        PYQResponse dto = new PYQResponse();
        dto.id = p.getId();
        dto.role = p.role;
        dto.roundType = p.roundType != null ? p.roundType.name() : null;
        dto.year = p.year;
        dto.content = p.content;
        dto.difficulty = p.difficulty;
        dto.tags = p.tags;
        dto.upvotes = p.upvotes;
        dto.anonymous = p.anonymous;
        if (p.company != null) {
            dto.companyName = p.company.name;
        }
        if (!p.anonymous && p.contributedByStudent != null && p.contributedByStudent.user != null) {
            dto.contributedBy = p.contributedByStudent.user.name;
        }
        return dto;
    }
}

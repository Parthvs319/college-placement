package student;

import lombok.Data;
import models.sql.*;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Shared DTOs for all Student portal responses.
 */
public final class StudentDtos {

    private StudentDtos() {}

    // ── Student Profile ──

    @Data
    public static class ProfileResponse {
        Long id;
        // User-level
        String name;
        String email;
        String mobile;
        String avatarUrl;
        boolean verified;
        // Student core
        String enrollmentNumber;
        String studentCollegeId;
        String department;
        int passingYear;
        String category;
        // Academic
        BigDecimal cgpa;
        int activeBacklogs;
        int totalBacklogs;
        String tenthPercentage;
        String twelfthPercentage;
        String diplomaPercentage;
        // Personal
        String gender;
        String dateOfBirth;
        // Identity docs
        String aadharNumber;
        String panNumber;
        // Online presence
        List<String> skills;
        List<String> certifications;
        String linkedinUrl;
        String githubUrl;
        String portfolioUrl;
        String resumeUrl;
        // Status
        boolean placed;
        boolean optedOut;
        boolean internship;
        boolean ppo;
        BigDecimal currentCtc;
        boolean profileComplete;
        Timestamp profileSubmittedAt;
        // College
        String collegeName;
    }

    public static ProfileResponse toProfileDto(Student s) {
        ProfileResponse dto = new ProfileResponse();
        dto.id = s.getId();
        dto.enrollmentNumber = s.getEnrollmentNumber();
        dto.studentCollegeId = s.getStudentCollegeId();
        dto.department       = s.getDepartment();
        dto.passingYear      = s.getPassingYear();
        dto.category         = s.getCategory();
        dto.cgpa             = s.getCgpa();
        dto.activeBacklogs   = s.getActiveBacklogs();
        dto.totalBacklogs    = s.getTotalBacklogs();
        dto.tenthPercentage  = s.getTenthPercentage();
        dto.twelfthPercentage  = s.getTwelfthPercentage();
        dto.diplomaPercentage  = s.getDiplomaPercentage();
        dto.gender           = s.getGender();
        dto.dateOfBirth      = s.getDateOfBirth();
        dto.aadharNumber     = s.getAadharNumber();
        dto.panNumber        = s.getPanNumber();
        dto.skills           = s.getSkills();
        dto.certifications   = s.getCertifications();
        dto.linkedinUrl      = s.getLinkedinUrl();
        dto.githubUrl        = s.getGithubUrl();
        dto.portfolioUrl     = s.getPortfolioUrl();
        dto.resumeUrl        = s.getResumeUrl();
        dto.placed           = s.isPlaced();
        dto.optedOut         = s.isOptedOut();
        dto.internship       = s.isInternship();
        dto.ppo              = s.isPpo();
        dto.currentCtc       = s.getCurrentCtc();
        dto.profileComplete  = s.isProfileComplete();
        dto.profileSubmittedAt = s.getProfileSubmittedAt();

        User user = s.getUser();
        if (user != null) {
            dto.name      = user.getName();
            dto.email     = user.getEmail();
            dto.mobile    = user.getMobile();
            dto.avatarUrl = user.getAvatarUrl();
            dto.verified  = user.isVerified();
        }
        College college = s.getCollege();
        if (college != null) {
            dto.collegeName = college.getName();
        }
        return dto;
    }

    // ── Drive (student view) ──

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
        dto.title           = d.getTitle();
        dto.employmentType  = d.getEmploymentType() != null ? d.getEmploymentType().name() : null;
        dto.status          = d.getStatus() != null ? d.getStatus().name() : null;
        dto.ctcOffered      = d.getCtcOffered();
        dto.stipend         = d.getStipend();
        dto.location        = d.getLocation();
        dto.isRemote        = d.isRemote();
        dto.minCgpa         = d.getMinCgpa();
        dto.maxActiveBacklogs = d.getMaxActiveBacklogs();
        dto.eligibleDepartments = d.getEligibleDepartments();
        dto.minPassingYear  = d.getMinPassingYear();
        dto.maxPassingYear  = d.getMaxPassingYear();
        dto.registrationDeadline = d.getRegistrationDeadline();
        dto.driveDate       = d.getDriveDate();
        dto.venue           = d.getVenue();
        CompanyCollege cc = d.getCompanyCollege();
        if (cc != null && cc.getCompany() != null) {
            dto.companyName    = cc.getCompany().getName();
            dto.companyLogoUrl = cc.getCompany().getLogoUrl();
        }
        return dto;
    }

    // ── Drive Detail ──

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
        dto.id             = d.getId();
        dto.title          = d.getTitle();
        dto.jobDescription = d.getJobDescription();
        dto.employmentType = d.getEmploymentType() != null ? d.getEmploymentType().name() : null;
        dto.status         = d.getStatus() != null ? d.getStatus().name() : null;
        dto.ctcOffered     = d.getCtcOffered();
        dto.stipend        = d.getStipend();
        dto.location       = d.getLocation();
        dto.isRemote       = d.isRemote();
        dto.minCgpa        = d.getMinCgpa();
        dto.maxActiveBacklogs = d.getMaxActiveBacklogs();
        dto.eligibleDepartments = d.getEligibleDepartments();
        dto.minPassingYear = d.getMinPassingYear();
        dto.maxPassingYear = d.getMaxPassingYear();
        dto.requiredSkills   = d.getRequiredSkills();
        dto.niceToHaveSkills = d.getNiceToHaveSkills();
        dto.registrationDeadline = d.getRegistrationDeadline();
        dto.driveDate      = d.getDriveDate();
        dto.venue          = d.getVenue();
        CompanyCollege cc = d.getCompanyCollege();
        if (cc != null && cc.getCompany() != null) {
            dto.companyName    = cc.getCompany().getName();
            dto.companyLogoUrl = cc.getCompany().getLogoUrl();
            dto.companyWebsite = cc.getCompany().getWebsite();
        }
        return dto;
    }

    // ── Application ──

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
        dto.id             = a.getId();
        dto.status         = a.getStatus() != null ? a.getStatus().name() : null;
        dto.resumeSnapshot = a.getResumeSnapshot();
        dto.notes          = a.getNotes();
        dto.appliedAt      = a.getCreatedAt();
        Drive drive = a.getDrive();
        if (drive != null) {
            dto.driveId   = drive.getId();
            dto.driveTitle = drive.getTitle();
            dto.driveDate  = drive.getDriveDate();
            CompanyCollege cc = drive.getCompanyCollege();
            if (cc != null && cc.getCompany() != null) {
                dto.companyName = cc.getCompany().getName();
            }
        }
        return dto;
    }

    // ── Offer ──

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
        dto.id               = o.getId();
        dto.ctcOffered       = o.getCtcOffered();
        dto.designation      = o.getDesignation();
        dto.location         = o.getLocation();
        dto.status           = o.getStatus() != null ? o.getStatus().name() : null;
        dto.responseDeadline = o.getResponseDeadline();
        dto.respondedAt      = o.getRespondedAt();
        dto.offerLetterUrl   = o.getOfferLetterUrl();
        dto.notes            = o.getNotes();
        dto.createdAt        = o.getCreatedAt();
        Drive drive = o.getDrive();
        if (drive != null) {
            dto.driveId    = drive.getId();
            dto.driveTitle = drive.getTitle();
            CompanyCollege cc = drive.getCompanyCollege();
            if (cc != null && cc.getCompany() != null) {
                dto.companyName = cc.getCompany().getName();
            }
        }
        return dto;
    }

    // ── Student Document ──

    @Data
    public static class DocumentResponse {
        Long id;
        String documentType;
        String label;
        String fileName;
        String fileUrl;
        String contentType;
        Long fileSizeBytes;
        Integer semester;
        boolean verified;
        String verificationNote;
        String createdAt;
    }

    public static DocumentResponse toDocumentDto(StudentDocument d) {
        DocumentResponse dto = new DocumentResponse();
        dto.id               = d.getId();
        dto.documentType     = d.getDocumentType();
        dto.label            = d.getLabel();
        dto.fileName         = d.getFileName();
        dto.fileUrl          = d.getFileUrl();
        dto.contentType      = d.getContentType();
        dto.fileSizeBytes    = d.getFileSizeBytes();
        dto.semester         = d.getSemester();
        dto.verified         = d.isVerified();
        dto.verificationNote = d.getVerificationNote();
        dto.createdAt        = d.getCreatedAt() != null ? d.getCreatedAt().toString() : null;
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
        dto.id         = p.getId();
        dto.role       = p.getRole();
        dto.roundType  = p.getRoundType() != null ? p.getRoundType().name() : null;
        dto.year       = p.getYear();
        dto.content    = p.getContent();
        dto.difficulty = p.getDifficulty();
        dto.tags       = p.getTags();
        dto.upvotes    = p.getUpvotes();
        dto.anonymous  = p.isAnonymous();
        Company company = p.getCompany();
        if (company != null) {
            dto.companyName = company.getName();
        }
        if (!p.isAnonymous() && p.getContributedByStudent() != null && p.getContributedByStudent().getUser() != null) {
            dto.contributedBy = p.getContributedByStudent().getUser().getName();
        }
        return dto;
    }
}

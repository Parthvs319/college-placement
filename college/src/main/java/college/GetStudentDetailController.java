package college;

import helpers.annotations.CollegeRole;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.ebean.DB;
import io.vertx.rxjava.ext.web.RoutingContext;
import lombok.Data;
import models.access.middlewear.college.CollegeAccessMiddleware;
import models.body.CollegeLoginRequest;
import models.repos.StudentDocumentRepository;
import models.repos.StudentRepository;
import models.sql.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * GET /college/students/:studentId
 * Returns full student detail — only if the student belongs to this college.
 */
@CollegeRole
public enum GetStudentDetailController implements BaseController {

    INSTANCE;

    @Override
    public void handle(RoutingContext event) {
        CollegeAccessMiddleware.INSTANCE.with(event, new ArrayList<>(), this.getClass())
                .map(req -> build(req, event))
                .subscribe(
                        o -> ResponseUtils.INSTANCE.writeJsonResponse(event, o),
                        error -> ResponseUtils.INSTANCE.handleError(event, error)
                );
    }

    private StudentDetail build(CollegeLoginRequest req, RoutingContext event) {
        String idParam = event.request().getParam("studentId");
        if (idParam == null) throw new RoutingError("studentId is required");

        long studentId;
        try {
            studentId = Long.parseLong(idParam);
        } catch (NumberFormatException e) {
            throw new RoutingError("Invalid studentId");
        }

        Student student = StudentRepository.INSTANCE.byId(studentId);
        if (student == null) throw new RoutingError("Student not found");

        // Security: student must belong to this college
        if (student.getCollege() == null || !student.getCollege().getId().equals(req.getCollege().getId())) {
            throw new RoutingError("Student not found");
        }

        StudentDetail detail = new StudentDetail();
        User user = student.getUser();

        detail.id             = student.getId();
        detail.name           = user != null ? user.getName()   : null;
        detail.email          = user != null ? user.getEmail()  : null;
        detail.mobile         = user != null ? user.getMobile() : null;
        detail.verified       = user != null && user.isVerified();
        detail.active         = user != null && user.isActive();
        detail.profileComplete = student.isProfileComplete();

        // Academic
        detail.enrollmentNumber   = student.getEnrollmentNumber();
        detail.studentCollegeId   = student.getStudentCollegeId();
        detail.department         = student.getDepartment();
        detail.passingYear        = student.getPassingYear();
        detail.cgpa               = student.getCgpa();
        detail.activeBacklogs     = student.getActiveBacklogs();
        detail.totalBacklogs      = student.getTotalBacklogs();
        detail.tenthPercentage    = student.getTenthPercentage();
        detail.twelfthPercentage  = student.getTwelfthPercentage();
        detail.diplomaPercentage  = student.getDiplomaPercentage();
        detail.gender             = student.getGender();
        detail.category           = student.getCategory();
        detail.dateOfBirth        = student.getDateOfBirth();
        detail.aadharNumber       = student.getAadharNumber();
        detail.panNumber          = student.getPanNumber();

        // Skills / links
        detail.skills          = student.getSkills();
        detail.certifications  = student.getCertifications();
        detail.linkedinUrl     = student.getLinkedinUrl();
        detail.githubUrl       = student.getGithubUrl();
        detail.portfolioUrl    = student.getPortfolioUrl();
        detail.atsScore        = student.getAtsScore();

        // Placement
        detail.placed      = student.isPlaced();
        detail.placedAt    = student.getPlacedAt() != null ? student.getPlacedAt().toString() : null;
        detail.optedOut    = student.isOptedOut();
        detail.currentCtc  = student.getCurrentCtc();

        // Applications (drives this student applied to — only for drives at this college)
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
                ai.driveId    = app.getDrive().getId();
                ai.driveTitle = app.getDrive().getTitle();
                ai.ctcOffered = app.getDrive().getCtcOffered();
                ai.driveStatus = app.getDrive().getStatus() != null ? app.getDrive().getStatus().name() : null;
                if (app.getDrive().getCompanyCollege() != null
                        && app.getDrive().getCompanyCollege().getCompany() != null) {
                    ai.companyId   = app.getDrive().getCompanyCollege().getCompany().getId();
                    ai.companyName = app.getDrive().getCompanyCollege().getCompany().getName();
                }
            }
            ai.appliedAt = app.getCreatedAt() != null ? app.getCreatedAt().toString() : null;
            return ai;
        }).collect(Collectors.toList());

        // Offers
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
            oi.offerId       = o.getId();
            oi.ctcOffered    = o.getCtcOffered();
            oi.designation   = o.getDesignation();
            oi.location      = o.getLocation();
            oi.status        = o.getStatus() != null ? o.getStatus().name() : null;
            oi.offerLetterUrl = o.getOfferLetterUrl();
            if (o.getDrive() != null) {
                oi.driveId    = o.getDrive().getId();
                oi.driveTitle = o.getDrive().getTitle();
                if (o.getDrive().getCompanyCollege() != null
                        && o.getDrive().getCompanyCollege().getCompany() != null) {
                    oi.companyId   = o.getDrive().getCompanyCollege().getCompany().getId();
                    oi.companyName = o.getDrive().getCompanyCollege().getCompany().getName();
                }
            }
            oi.createdAt = o.getCreatedAt() != null ? o.getCreatedAt().toString() : null;
            return oi;
        }).collect(Collectors.toList());

        // Resumes
        if (student.getResumes() != null) {
            detail.resumeCount = student.getResumes().size();
            detail.resumes = student.getResumes().stream()
                    .filter(r -> !r.isDeleted())
                    .map(r -> {
                        ResumeInfo ri = new ResumeInfo();
                        ri.id          = r.getId();
                        ri.fileName    = r.getFileName();
                        ri.url         = r.getUrl();
                        ri.contentType = r.getContentType();
                        ri.fileSize    = r.getFileSize();
                        ri.primary     = r.isPrimary();
                        ri.label       = r.getLabel();
                        ri.atsScore    = r.getAtsScore();
                        ri.uploadedAt  = r.getCreatedAt() != null ? r.getCreatedAt().toString() : null;
                        return ri;
                    }).collect(Collectors.toList());
        }

        // Documents
        List<StudentDocument> docs = StudentDocumentRepository.INSTANCE.byStudentId(studentId);
        detail.documents = docs.stream().map(d -> {
            DocumentInfo di = new DocumentInfo();
            di.id               = d.getId();
            di.documentType     = d.getDocumentType();
            di.label            = d.getLabel();
            di.fileName         = d.getFileName();
            di.fileUrl          = d.getFileUrl();
            di.contentType      = d.getContentType();
            di.fileSizeBytes    = d.getFileSizeBytes();
            di.semester         = d.getSemester();
            di.verified         = d.isVerified();
            di.verificationNote = d.getVerificationNote();
            di.uploadedAt       = d.getCreatedAt() != null ? d.getCreatedAt().toString() : null;
            return di;
        }).collect(Collectors.toList());

        return detail;
    }

    @Data
    public static class StudentDetail {
        Long id;
        String name;
        String email;
        String mobile;
        boolean verified;
        boolean active;
        boolean profileComplete;

        // Academic
        String enrollmentNumber;
        String studentCollegeId;
        String department;
        int passingYear;
        BigDecimal cgpa;
        int activeBacklogs;
        int totalBacklogs;
        String tenthPercentage;
        String twelfthPercentage;
        String diplomaPercentage;
        String gender;
        String category;
        String dateOfBirth;
        String aadharNumber;
        String panNumber;

        // Skills / links
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

        // Relations
        List<ApplicationInfo> applications;
        List<OfferInfo> offers;

        // Resumes
        int resumeCount;
        List<ResumeInfo> resumes;

        // Documents
        List<DocumentInfo> documents;
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
    public static class ResumeInfo {
        Long id;
        String fileName;
        String url;
        String contentType;
        long fileSize;
        boolean primary;
        String label;
        Integer atsScore;
        String uploadedAt;
    }

    @Data
    public static class DocumentInfo {
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
        String uploadedAt;
    }
}

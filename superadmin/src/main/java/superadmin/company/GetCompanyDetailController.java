package superadmin.company;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.Data;

/**
 * GET /admin/companies/:companyId
 * Returns detailed info about a company including linked colleges,
 * drives conducted, offers given, and HR users.
 */
@SuperAdminRole
public enum GetCompanyDetailController implements BaseController {

    INSTANCE;

    @Override
    public void handle(RoutingContext event) {
        SuperAdminAccessMiddleware.INSTANCE.with(event, new ArrayList<>(), this.getClass())
                .map(req -> {
                    Long companyId = Long.parseLong(event.pathParam("companyId"));
                    Company company = CompanyRepository.INSTANCE.byId(companyId);
                    if (company == null) throw new RoutingError(404, "Company not found");

                    CompanyDetail detail = new CompanyDetail();

                    // Basic info
                    detail.id = company.getId();
                    detail.name = company.getName();
                    detail.industry = company.getIndustry();
                    detail.website = company.getWebsite();
                    detail.logoUrl = company.getLogoUrl();
                    detail.description = company.getDescription();
                    detail.headquarters = company.getHeadquarters();
                    detail.contactEmail = company.getContactEmail();
                    detail.contactPhone = company.getContactPhone();
                    detail.startup = company.isStartup();
                    detail.active = company.isActive();

                    // Linked colleges
                    List<CompanyCollege> links = CompanyCollegeRepository.INSTANCE.byCompany(companyId);
                    detail.linkedColleges = links.stream()
                            .filter(cc -> cc.getCollege() != null)
                            .map(cc -> {
                                CollegeLink cl = new CollegeLink();
                                cl.collegeId = cc.getCollege().getId();
                                cl.collegeName = cc.getCollege().getName();
                                cl.collegeCode = cc.getCollege().getCode();
                                cl.active = cc.isActive();
                                return cl;
                            })
                            .collect(Collectors.toList());
                    detail.totalCollegesLinked = detail.linkedColleges.size();

                    // HR users linked to this company
                    List<User> hrUsers = UserRepository.INSTANCE.findAll().stream()
                            .filter(u -> u.getCompany() != null && u.getCompany().getId().equals(companyId))
                            .collect(Collectors.toList());
                    detail.hrUsers = hrUsers.stream().map(u -> {
                        HrUser hu = new HrUser();
                        hu.id = u.getId();
                        hu.name = u.getName();
                        hu.email = u.getEmail();
                        hu.verified = u.isVerified();
                        hu.active = u.isActive();
                        return hu;
                    }).collect(Collectors.toList());

                    // Drives and stats
                    List<Drive> allDrives = DriveRepository.INSTANCE.byCompany(companyId);
                    int totalOffers = 0;
                    int totalApplications = 0;
                    BigDecimal highestCtc = BigDecimal.ZERO;

                    List<DriveInfo> driveInfos = new ArrayList<>();
                    for (Drive d : allDrives) {
                        DriveInfo di = new DriveInfo();
                        di.driveId = d.getId();
                        di.title = d.getTitle();
                        di.status = d.getStatus() != null ? d.getStatus().name() : null;
                        di.ctcOffered = d.getCtcOffered();
                        di.employmentType = d.getEmploymentType() != null ? d.getEmploymentType().name() : null;
                        di.driveDate = d.getDriveDate() != null ? d.getDriveDate().toString() : null;

                        // Find college name via company_college
                        if (d.getCompanyCollege() != null && d.getCompanyCollege().getCollege() != null) {
                            di.collegeName = d.getCompanyCollege().getCollege().getName();
                        }

                        int appCount = d.getApplications() != null ? d.getApplications().size() : 0;
                        int offCount = d.getOffers() != null ? d.getOffers().size() : 0;
                        di.applicationCount = appCount;
                        di.offerCount = offCount;

                        totalApplications += appCount;
                        totalOffers += offCount;

                        if (d.getCtcOffered() != null && d.getCtcOffered().compareTo(highestCtc) > 0) {
                            highestCtc = d.getCtcOffered();
                        }

                        driveInfos.add(di);
                    }

                    detail.totalDrives = driveInfos.size();
                    detail.activeDrives = (int) driveInfos.stream()
                            .filter(d -> d.status != null
                                    && !d.status.equals("COMPLETED")
                                    && !d.status.equals("CANCELLED"))
                            .count();
                    detail.totalOffers = totalOffers;
                    detail.totalApplications = totalApplications;
                    detail.highestCtcOffered = highestCtc;
                    detail.recentDrives = driveInfos.stream().limit(10).collect(Collectors.toList());

                    // Company documents (Applyra contract, GST, incorporation, NDA, etc.)
                    List<CompanyDocument> companyDocs = CompanyDocumentRepository.INSTANCE.byCompanyId(companyId);
                    detail.documents = companyDocs.stream().map(doc -> {
                        DocumentInfo di = new DocumentInfo();
                        di.id = doc.getId();
                        di.documentType = doc.getDocumentType();
                        di.label = doc.getLabel();
                        di.fileName = doc.getFileName();
                        di.fileUrl = doc.getFileUrl();
                        di.contentType = doc.getContentType();
                        di.fileSizeBytes = doc.getFileSizeBytes();
                        di.expiryDate = doc.getExpiryDate();
                        di.verified = doc.isVerified();
                        di.verificationNote = doc.getVerificationNote();
                        di.uploadedAt = doc.getCreatedAt() != null ? doc.getCreatedAt().toString() : null;
                        return di;
                    }).collect(Collectors.toList());

                    return detail;
                })
                .subscribe(
                        o -> ResponseUtils.INSTANCE.writeJsonResponse(event, o),
                        error -> ResponseUtils.INSTANCE.handleError(event, error)
                );
    }

    @Data
    public static class CompanyDetail {
        Long id;
        String name;
        String industry;
        String website;
        String logoUrl;
        String description;
        String headquarters;
        String contactEmail;
        String contactPhone;
        boolean startup;
        boolean active;

        // Linked colleges
        List<CollegeLink> linkedColleges;
        int totalCollegesLinked;

        // HR users
        List<HrUser> hrUsers;

        // Stats
        int totalDrives;
        int activeDrives;
        int totalOffers;
        int totalApplications;
        BigDecimal highestCtcOffered;

        // Recent drives
        List<DriveInfo> recentDrives;

        // Documents
        List<DocumentInfo> documents;
    }

    @Data
    public static class CollegeLink {
        Long collegeId;
        String collegeName;
        String collegeCode;
        boolean active;
    }

    @Data
    public static class HrUser {
        Long id;
        String name;
        String email;
        boolean verified;
        boolean active;
    }

    @Data
    public static class DriveInfo {
        Long driveId;
        String title;
        String collegeName;
        String status;
        BigDecimal ctcOffered;
        String employmentType;
        String driveDate;
        int applicationCount;
        int offerCount;
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
        String expiryDate;
        boolean verified;
        String verificationNote;
        String uploadedAt;
    }
}

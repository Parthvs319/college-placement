package company;

import io.ebean.DB;
import lombok.Data;
import models.sql.*;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

/**
 * Shared DTOs for all Company portal responses.
 */
public final class CompanyDtos {

    private CompanyDtos() {}

    // ── Company Profile ──

    @Data
    public static class CompanyResponse {
        Long id;
        String name;
        String industry;
        String website;
        String logoUrl;
        String description;
        String headquarters;
        String contactEmail;
        String contactPhone;
        boolean active;
    }

    public static CompanyResponse toCompanyDto(Company c) {
        CompanyResponse dto = new CompanyResponse();
        dto.id = c.getId();
        dto.name = c.name;
        dto.industry = c.industry;
        dto.website = c.website;
        dto.logoUrl = c.logoUrl;
        dto.description = c.description;
        dto.headquarters = c.headquarters;
        dto.contactEmail = c.contactEmail;
        dto.contactPhone = c.contactPhone;
        dto.active = c.active;
        return dto;
    }

    // ── Linked College (company's view of which colleges they're connected to) ──

    @Data
    public static class LinkedCollegeResponse {
        Long id;
        Long collegeId;
        String collegeName;
        String collegeCity;
        String collegeState;
        boolean companyCanManage;
        boolean active;
    }

    public static LinkedCollegeResponse toLinkedCollegeDto(CompanyCollege cc) {
        LinkedCollegeResponse dto = new LinkedCollegeResponse();
        dto.id = cc.getId();
        dto.companyCanManage = cc.companyCanManage;
        dto.active = cc.active;
        if (cc.college != null) {
            dto.collegeId = cc.college.getId();
            dto.collegeName = cc.college.name;
            if (cc.college.cityId != null) {
                City ct = DB.find(City.class, cc.college.cityId);
                if (ct != null) dto.collegeCity = ct.name;
            }
            if (cc.college.stateId != null) {
                States st = DB.find(States.class, cc.college.stateId);
                if (st != null) dto.collegeState = st.name;
            }
        }
        return dto;
    }

    // ── Drive (company's view across colleges) ──

    @Data
    public static class CompanyDriveResponse {
        Long id;
        String title;
        String employmentType;
        String status;
        String collegeName;
        BigDecimal ctcOffered;
        BigDecimal stipend;
        String location;
        boolean isRemote;
        Timestamp registrationDeadline;
        Timestamp driveDate;
        Timestamp createdAt;
    }

    public static CompanyDriveResponse toCompanyDriveDto(Drive d) {
        CompanyDriveResponse dto = new CompanyDriveResponse();
        dto.id = d.getId();
        dto.title = d.title;
        dto.employmentType = d.employmentType != null ? d.employmentType.name() : null;
        dto.status = d.status != null ? d.status.name() : null;
        dto.ctcOffered = d.ctcOffered;
        dto.stipend = d.stipend;
        dto.location = d.location;
        dto.isRemote = d.isRemote;
        dto.registrationDeadline = d.registrationDeadline;
        dto.driveDate = d.driveDate;
        dto.createdAt = d.getCreatedAt();
        if (d.companyCollege != null && d.companyCollege.college != null) {
            dto.collegeName = d.companyCollege.college.name;
        }
        return dto;
    }
}

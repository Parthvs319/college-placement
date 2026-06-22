package superadmin;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * DTOs for all Super Admin API responses.
 */
public class SuperAdminDtos {

    @Data
    public static class PlatformAnalytics {
        int totalColleges;
        int activeColleges;
        int totalStudents;
        int placedStudents;
        int unplacedStudents;
        int totalCompanies;
        int startupCount;
        int totalDrives;
        int activeDrives;
        int totalOffers;
        int totalUsers;
        double overallPlacementRate;
        BigDecimal averageCtc;
        BigDecimal highestCtc;
        BigDecimal lowestCtc;
    }

    @Data
    public static class CollegeSummary {
        Long id;
        String name;
        String code;
        String city;
        String state;
        String university;
        boolean verified;
        boolean active;
        int studentCount;
        int placedCount;
        int driveCount;
        int companyCount;
        int startupCount;
        double placementRate;
        // Verification + GSTIN
        String gstin;
        String tpoName;
        boolean isEmailVerified;
        boolean isPhoneVerified;
        // Active contract summary
        String contractEndDate;   // validTo from latest active contract
        String contractType;      // "PAID" | "FREE_TRIAL"
        // Drive type breakdown
        int internshipCount;
        int fullTimeCount;
    }

    @Data
    public static class ContractRenewalItem {
        Long collegeId;
        String collegeName;
        String collegeCode;
        String contactEmail;
        String tpoName;
        String contractType;   // "PAID" | "FREE_TRIAL"
        String validFrom;
        String validTo;
        int daysRemaining;
        Long contractId;
    }

    @Data
    public static class UserSummary {
        Long id;
        String name;
        String email;
        String mobile;
        String userType;
        boolean verified;
        boolean active;
        String collegeName;
        Long collegeId;
        Long studentId;
        Long companyId;
        String companyName;
        String createdAt;

        // Student-specific fields
        boolean placed;
        String placedCompanyName;
        BigDecimal currentCtc;
        String department;
        int passingYear;
        BigDecimal cgpa;

        // HR-specific fields
        int collegeAssociatedCount;
        int totalStudentsPicked;
        BigDecimal hrHighestCtc;
        BigDecimal hrLowestCtc;
        int hrDriveCount;

        // TPO / College Admin specific fields
        int totalFinalYearStudents;
        int totalPlaced;
        int totalUnplaced;
        int totalCompaniesOnboarded;
        BigDecimal tpoHighestCtc;
        BigDecimal tpoLowestCtc;
        int internshipCount;
        int fullTimeOfferCount;
    }

    @Data
    public static class DriveOverview {
        Long id;
        String title;
        String driveCode;
        String companyName;
        String collegeName;
        String status;
        String employmentType;
        BigDecimal ctcOffered;
        int applicationCount;
        int offerCount;
        String driveDate;
    }

    @Data
    public static class OfferOverview {
        Long id;
        String studentName;
        String studentEmail;
        String companyName;
        String collegeName;
        String driveName;
        BigDecimal ctc;
        String status;
        String createdAt;
    }

    @Data
    public static class CompanySummary {
        Long id;
        String name;
        String code;
        String industry;
        String website;
        boolean startup;
        int collegeCount;
        int driveCount;
        int totalOffers;
    }

    @Data
    public static class StudentOverview {
        Long id;
        String name;
        String email;
        String department;
        BigDecimal cgpa;
        String collegeName;
        Long collegeId;
        boolean verified;
        boolean placed;
        String placedAt;
        int applicationCount;
        int offerCount;
    }

    @Data
    public static class SubscriptionOverview {
        Long id;
        String tier;
        boolean active;
        String studentName;
        String collegeName;
        String startDate;
        String endDate;
        int totalCredits;
        int usedCredits;
        int remainingCredits;
    }

    @Data
    public static class ActivityEvent {
        String type;        // "college_added", "drive_created", "offer_made", "application_submitted", "user_registered", "college_verified", "drive_completed"
        String title;       // short headline e.g. "New drive created"
        String description; // detail e.g. "Google SDE Intern at SGSITS"
        String timestamp;   // ISO string
        String color;       // "green", "blue", "amber", "purple"
    }

    @Data
    public static class CreditTransactionDto {
        Long id;
        String type;            // CreditTransactionType name
        int amount;             // positive for top-up, negative for usage
        int balanceAfter;
        String description;
        String paymentReference;
        String studentName;
        String collegeName;
        String createdAt;
    }

    /** Slim record for client-side Fuse.js index — only the 4 fields needed for search */
    @Data
    public static class AisheSlimRecord {
        String aisheCode;
        String name;
        String state;
        String district;
    }

    @Data
    public static class AisheCollegeResult {
        Long id;
        String aisheCode;
        String name;
        String state;
        String district;
        String website;
        Integer yearOfEstablishment;
        String location;
        String collegeType;
        String management;
        String universityAisheCode;
        String universityName;
        String universityType;
    }

    /**
     * A contract whose next invoice cycle is approaching.
     * Returned by GET /admin/invoice-due
     */
    @Data
    public static class InvoiceDueItem {
        Long contractId;
        Long collegeId;
        String collegeName;
        String collegeCode;
        String contactEmail;
        String payType;            // MONTHLY | YEARLY
        String validFrom;
        String validTo;
        String nextInvoiceDate;    // ISO date of the upcoming invoice cycle
        int daysUntilDue;          // days from today until nextInvoiceDate
        String billingPeriodStart; // suggested billing period start
        String billingPeriodEnd;   // suggested billing period end (day before nextInvoiceDate)
        String contractAmountDisplay; // e.g. "₹50,000"
    }
}

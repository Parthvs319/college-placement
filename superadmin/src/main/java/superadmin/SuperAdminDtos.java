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
    }

    @Data
    public static class DriveOverview {
        Long id;
        String title;
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
}

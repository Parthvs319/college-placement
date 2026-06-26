package college;

import helpers.annotations.CollegeRole;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.PasswordUtils;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.college.CollegeAccessMiddleware;
import models.body.CollegeLoginRequest;
import models.enums.UserType;
import models.repos.StudentRepository;
import models.repos.UserRepository;
import models.services.EmailService;
import models.sql.Student;
import models.sql.User;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.*;

/**
 * TPO bulk-uploads a list of students as JSON array.
 * Creates User (STUDENT) + Student records with auto-generated passwords.
 * Sends credentials email to each student.
 *
 * POST /college/students/bulk-upload
 * Body: { "students": [
 *   { "name": "John Doe", "email": "john@gmail.com", "enrollmentNumber": "0901CS201001",
 *     "department": "CSE", "passingYear": 2025 },
 *   ...
 * ]}
 */
@CollegeRole
public enum BulkUploadStudentsController implements BaseController {

    INSTANCE;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";

    @Override
    public void handle(RoutingContext event) {
        CollegeAccessMiddleware.INSTANCE.with(event, new ArrayList<>(), this.getClass())
                .map(this::map)
                .subscribe(
                        o -> ResponseUtils.INSTANCE.writeJsonResponse(event, o),
                        error -> ResponseUtils.INSTANCE.handleError(event, error)
                );
    }

    private Object map(CollegeLoginRequest request) {
        io.vertx.core.json.JsonObject body = request.getRoutingContext().getBodyAsJson();
        if (body == null || !body.containsKey("students") || body.getJsonArray("students") == null) {
            throw new RoutingError("students array is required");
        }

        List<?> studentsList = body.getJsonArray("students").getList();
        if (studentsList.isEmpty()) {
            throw new RoutingError("students array cannot be empty");
        }
        if (studentsList.size() > 200) {
            throw new RoutingError("Maximum 200 students at a time");
        }

        Long collegeId = request.getCollege().getId();
        String collegeName = request.getCollege().getName();
        String collegeCode = request.getCollege().getCode() != null ? request.getCollege().getCode() : "";
        String tpoName  = request.getUser() != null && request.getUser().getName()  != null ? request.getUser().getName()  : "TPO";
        String tpoEmail = request.getUser() != null && request.getUser().getEmail() != null ? request.getUser().getEmail() : "";

        List<Map<String, Object>> results = new ArrayList<>();
        int created = 0;
        int skipped = 0;
        int failed = 0;

        for (Object obj : studentsList) {
            if (!(obj instanceof Map)) {
                failed++;
                continue;
            }
            Map<?, ?> row = (Map<?, ?>) obj;

            String name             = getStr(row, "name");
            String email            = getStr(row, "email");
            String enrollmentNumber = getStr(row, "enrollmentNumber");
            String department       = getStr(row, "department");
            int    passingYear      = getInt(row, "passingYear", 0);
            String mobile           = getStr(row, "mobile");
            String dateOfBirth      = getStr(row, "dateOfBirth");
            String tenthPct         = getStr(row, "tenthPercentage");
            String twelfthPct       = getStr(row, "twelfthPercentage");
            String diplomaPct       = getStr(row, "diplomaPercentage");
            int    activeBacklogs   = getInt(row, "activeBacklogs", 0);
            int    totalBacklogs    = getInt(row, "totalBacklogs", 0);
            String category         = getStr(row, "category");
            String aadharNumber     = getStr(row, "aadharNumber");
            String panNumber        = getStr(row, "panNumber");
            String studentCollegeId = getStr(row, "studentCollegeId");

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("email", email);
            item.put("enrollmentNumber", enrollmentNumber);

            // Validation
            if (email == null || email.isBlank()) {
                item.put("status", "failed");
                item.put("reason", "Email is required");
                results.add(item);
                failed++;
                continue;
            }
            if (name == null || name.isBlank()) {
                item.put("status", "failed");
                item.put("reason", "Name is required");
                results.add(item);
                failed++;
                continue;
            }
            if (enrollmentNumber == null || enrollmentNumber.isBlank()) {
                item.put("status", "failed");
                item.put("reason", "Enrollment number is required");
                results.add(item);
                failed++;
                continue;
            }

            // Aadhar validation: exactly 12 digits
            if (aadharNumber != null && !aadharNumber.isBlank()) {
                String cleanAadhar = aadharNumber.replaceAll("\\s+", "").replaceAll("-", "");
                if (!cleanAadhar.matches("\\d{12}")) {
                    item.put("status", "failed");
                    item.put("reason", "Aadhar number must be exactly 12 digits (got: " + aadharNumber.trim() + ")");
                    results.add(item);
                    failed++;
                    continue;
                }
                aadharNumber = cleanAadhar;
            }

            // PAN validation: format ABCDE1234F (5 alpha + 4 digits + 1 alpha)
            if (panNumber != null && !panNumber.isBlank()) {
                String cleanPan = panNumber.trim().toUpperCase().replaceAll("\\s+", "");
                if (!cleanPan.matches("[A-Z]{5}[0-9]{4}[A-Z]{1}")) {
                    item.put("status", "failed");
                    item.put("reason", "PAN number must be in format ABCDE1234F (got: " + panNumber.trim() + ")");
                    results.add(item);
                    failed++;
                    continue;
                }
                panNumber = cleanPan;
            }

            email = email.trim().toLowerCase();

            // Check if email already exists
            User existingUser = UserRepository.INSTANCE.byEmail(email);
            if (existingUser != null) {
                item.put("status", "skipped");
                item.put("reason", "Email already registered");
                results.add(item);
                skipped++;
                continue;
            }

            // Check if enrollment number already exists at this college
            Student existingStudent = StudentRepository.INSTANCE.byEnrollment(enrollmentNumber, collegeId);
            if (existingStudent != null) {
                item.put("status", "skipped");
                item.put("reason", "Enrollment number already exists at this college");
                results.add(item);
                skipped++;
                continue;
            }

            // Create User — NOT verified yet; student must complete profile first
            String rawPassword = generatePassword(10);
            User user = new User();
            user.email = email;
            user.name = name.trim();
            user.password = PasswordUtils.INSTANCE.hash(rawPassword);
            user.userType = UserType.STUDENT;
            user.college = request.getCollege();
            user.verified = false;  // student must complete profile to get verified
            user.active = true;
            if (mobile != null && !mobile.isBlank()) user.mobile = mobile.trim();
            user.save();

            // Create Student
            Student student = new Student();
            student.user = user;
            student.college = request.getCollege();
            student.enrollmentNumber = enrollmentNumber.trim();
            student.department = department != null ? department.trim() : null;
            student.passingYear = passingYear;

            // Optional academic fields
            String cgpaStr = getStr(row, "cgpa");
            if (cgpaStr != null && !cgpaStr.isBlank()) {
                try { student.cgpa = new BigDecimal(cgpaStr.trim()); } catch (NumberFormatException ignored) {}
            }
            student.gender          = getStr(row, "gender");
            student.dateOfBirth     = dateOfBirth != null && !dateOfBirth.isBlank() ? dateOfBirth.trim() : null;
            student.tenthPercentage = tenthPct != null && !tenthPct.isBlank() ? tenthPct.trim() : null;
            student.twelfthPercentage = twelfthPct != null && !twelfthPct.isBlank() ? twelfthPct.trim() : null;
            student.diplomaPercentage = diplomaPct != null && !diplomaPct.isBlank() ? diplomaPct.trim() : null;
            student.activeBacklogs  = activeBacklogs;
            student.totalBacklogs   = totalBacklogs;

            // Identity fields
            student.aadharNumber     = aadharNumber != null && !aadharNumber.isBlank() ? aadharNumber.trim() : null;
            student.panNumber        = panNumber != null && !panNumber.isBlank() ? panNumber.trim().toUpperCase() : null;
            student.studentCollegeId = studentCollegeId != null && !studentCollegeId.isBlank() ? studentCollegeId.trim() : null;
            student.category         = category != null && !category.isBlank() ? category.trim().toUpperCase() : null;

            student.save();

            // Send rich credentials email on separate thread
            final String finalEmail = email;
            final String finalName = name.trim();
            final String finalDept = department;
            final int finalYear = passingYear;
            new Thread(() -> {
                try {
                    String html = EmailService.buildStudentCredentialsHtml(
                            collegeName, finalName, finalEmail, rawPassword,
                            enrollmentNumber, finalDept, finalYear, tpoName
                    );
                    EmailService.sendEmail(finalEmail, "Your Applyra Student Login — " + collegeName, html)
                            .subscribe(
                                    sent -> System.out.println("[BulkUpload] Credentials " + (sent ? "sent" : "failed") + " to " + finalEmail),
                                    err -> System.err.println("[BulkUpload] Email error: " + err.getMessage())
                            );
                } catch (Exception e) {
                    System.err.println("[BulkUpload] Email thread error: " + e.getMessage());
                }
            }, "bulk-upload-email-" + email).start();

            item.put("status", "created");
            item.put("name", name);
            item.put("studentId", student.getId());
            results.add(item);
            created++;
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "Bulk upload processed");
        response.put("total", studentsList.size());
        response.put("created", created);
        response.put("skipped", skipped);
        response.put("failed", failed);
        response.put("results", results);
        return response;
    }

    private String getStr(Map<?, ?> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : null;
    }

    private int getInt(Map<?, ?> map, String key, int defaultVal) {
        Object val = map.get(key);
        if (val == null) return defaultVal;
        try {
            return Integer.parseInt(val.toString());
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    private String generatePassword(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }
}

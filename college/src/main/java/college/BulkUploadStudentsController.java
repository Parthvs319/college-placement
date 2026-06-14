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
        Object studentsObj = request.getRequest().get("students");
        if (studentsObj == null || !(studentsObj instanceof List)) {
            throw new RoutingError("students array is required");
        }

        List<?> studentsList = (List<?>) studentsObj;
        if (studentsList.isEmpty()) {
            throw new RoutingError("students array cannot be empty");
        }
        if (studentsList.size() > 200) {
            throw new RoutingError("Maximum 200 students at a time");
        }

        Long collegeId = request.getCollege().getId();
        String collegeName = request.getCollege().getName();

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

            String name = getStr(row, "name");
            String email = getStr(row, "email");
            String enrollmentNumber = getStr(row, "enrollmentNumber");
            String department = getStr(row, "department");
            int passingYear = getInt(row, "passingYear", 0);

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

            // Create User
            String rawPassword = generatePassword(10);
            User user = new User();
            user.email = email;
            user.name = name.trim();
            user.password = PasswordUtils.INSTANCE.hash(rawPassword);
            user.userType = UserType.STUDENT;
            user.college = request.getCollege();
            user.verified = true;  // TPO-uploaded = pre-verified
            user.active = true;
            user.save();

            // Create Student
            Student student = new Student();
            student.user = user;
            student.college = request.getCollege();
            student.enrollmentNumber = enrollmentNumber.trim();
            student.department = department != null ? department.trim() : null;
            student.passingYear = passingYear;

            // Optional fields
            String cgpaStr = getStr(row, "cgpa");
            if (cgpaStr != null && !cgpaStr.isBlank()) {
                try {
                    student.cgpa = new BigDecimal(cgpaStr.trim());
                } catch (NumberFormatException ignored) {}
            }
            student.gender = getStr(row, "gender");
            student.tenthPercentage = getStr(row, "tenthPercentage");
            student.twelfthPercentage = getStr(row, "twelfthPercentage");
            student.save();

            // Send credentials email on separate thread
            final String finalEmail = email;
            new Thread(() -> {
                try {
                    String html = EmailService.buildStudentCredentialsHtml(
                            collegeName, finalEmail, rawPassword, enrollmentNumber
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

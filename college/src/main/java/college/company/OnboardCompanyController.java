package college.company;

import helpers.annotations.CollegeRole;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.PasswordUtils;
import helpers.utils.ResponseUtils;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.college.CollegeAccessMiddleware;
import models.body.CollegeLoginRequest;
import models.enums.UserType;
import models.repos.CompanyCollegeRepository;
import models.repos.CompanyRepository;
import models.repos.UserRepository;
import models.services.EmailService;
import models.sql.Company;
import models.sql.CompanyCollege;
import models.sql.User;

import java.security.SecureRandom;
import java.util.*;

/**
 * TPO onboards a company: creates Company + COMPANY_HR User + CompanyCollege link.
 * Sends credentials email to the HR and a notification to all superadmins + college users.
 *
 * POST /college/companies/onboard
 * Body: { name*, contactEmail*,
 *         companyType?, industry?, cin?, gstin?, yearOfEstablishment?,
 *         employeeCount?, linkedinUrl?, website?, headquarters?, description?,
 *         hrName?, hrDesignation?, hrLinkedin?, contactPhone? }
 *
 * NOTE: reads the raw JSON body directly (middleware Request wrapper only maps
 * pre-declared fields in @CollegeRole(request=...)).
 */
@CollegeRole
public enum OnboardCompanyController implements BaseController {

    INSTANCE;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";

    // GSTIN: 2-digit state code + 10-char PAN + 1-digit entity number + 'Z' + 1-char checksum
    private static final java.util.regex.Pattern GSTIN_PATTERN =
            java.util.regex.Pattern.compile("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$");

    @Override
    public void handle(RoutingContext event) {
        CollegeAccessMiddleware.INSTANCE.with(event, new ArrayList<>(), this.getClass())
                .map(req -> map(req, event))
                .subscribe(
                        o -> ResponseUtils.INSTANCE.writeJsonResponse(event, o),
                        error -> ResponseUtils.INSTANCE.handleError(event, error)
                );
    }

    private Object map(CollegeLoginRequest request, RoutingContext event) {
        JsonObject body = event.body().asJsonObject();
        if (body == null) body = new JsonObject();

        String name         = body.getString("name");
        String contactEmail = body.getString("contactEmail");

        if (name == null || name.isBlank())                 throw new RoutingError("Company name is required");
        if (contactEmail == null || contactEmail.isBlank()) throw new RoutingError("HR contact email is required");

        // GSTIN validation (optional field but validated when provided)
        String gstin = str(body, "gstin");
        if (gstin != null && !GSTIN_PATTERN.matcher(gstin.toUpperCase()).matches()) {
            throw new RoutingError("Invalid GSTIN format. Expected: 2-digit state code + PAN + entity digit + Z + checksum (15 chars, e.g. 27AAPFU0939F1ZV)");
        }
        if (gstin != null) gstin = gstin.toUpperCase();

        final String hrEmail     = contactEmail.trim().toLowerCase();
        final String companyName = name.trim();

        Long   collegeId   = request.getCollege().getId();
        String collegeName = request.getCollege().getName();
        String collegeCode = request.getCollege().getCode();

        // Check if company already exists
        Company existingCompany = CompanyRepository.INSTANCE.byName(companyName);
        Company company;
        boolean companyCreated = false;

        if (existingCompany != null) {
            company = existingCompany;
            CompanyCollege existingLink = CompanyCollegeRepository.INSTANCE.byCompanyAndCollege(company.getId(), collegeId);
            if (existingLink != null) {
                throw new RoutingError("Company '" + companyName + "' is already linked to your college");
            }
        } else {
            company = new Company();
            company.name         = companyName;
            company.contactEmail = hrEmail;
            company.active       = true;
            company.onboardedByCollege = true;

            company.industry     = str(body, "industry");
            company.website      = str(body, "website");
            company.contactPhone = str(body, "contactPhone");
            company.description  = str(body, "description");
            company.headquarters = str(body, "headquarters");

            company.companyType         = str(body, "companyType");
            company.cin                 = str(body, "cin");
            company.gstin               = gstin;
            company.yearOfEstablishment = body.getInteger("yearOfEstablishment");
            company.employeeCount       = str(body, "employeeCount");
            company.linkedinUrl         = str(body, "linkedinUrl");
            company.hrDesignation       = str(body, "hrDesignation");
            company.hrLinkedin          = str(body, "hrLinkedin");

            company.save();
            int companySeq = CompanyRepository.INSTANCE.countAll();
            // Code format: CMP-{COLLEGE_CODE}-{NNN}
            String safeCollegeCode = (collegeCode != null && !collegeCode.isBlank()) ? collegeCode : "APL";
            company.code = "CMP-" + safeCollegeCode + "-" + String.format("%03d", companySeq);
            company.update();
            companyCreated = true;
        }

        // Create or find COMPANY_HR user
        User hrUser = UserRepository.INSTANCE.byEmail(hrEmail);
        String rawPassword = null;
        boolean userCreated = false;

        if (hrUser == null) {
            rawPassword      = generatePassword(12);
            hrUser           = new User();
            hrUser.email     = hrEmail;
            hrUser.name      = body.getString("hrName") != null ? body.getString("hrName").trim() : companyName + " HR";
            hrUser.password  = PasswordUtils.INSTANCE.hash(rawPassword);
            hrUser.userType  = UserType.COMPANY_HR;
            hrUser.verified  = true;
            hrUser.active    = true;
            hrUser.isPrimary = true;
            hrUser.company   = company;
            hrUser.save();
            userCreated = true;
        }

        // Create CompanyCollege link
        CompanyCollege cc   = new CompanyCollege();
        cc.company          = company;
        cc.college          = request.getCollege();
        cc.managedByUser    = hrUser;
        cc.companyCanManage = true;
        cc.active           = true;
        cc.save();

        // ── Send emails asynchronously ───────────────────────────────────────
        final Company   finalCompany  = company;
        final String    finalPassword = rawPassword;
        final String    finalHrEmail  = hrEmail;
        final boolean   finalUserCreated = userCreated;
        final boolean   finalCompanyCreated = companyCreated;

        new Thread(() -> {
            try {
                String notificationHtml = EmailService.buildCompanyOnboardingNotificationHtml(
                        finalCompany.name, finalCompany.code, finalCompany.industry,
                        finalCompany.headquarters, finalHrEmail,
                        collegeName + " (TPO Portal)"
                );
                String subject = "New Company Onboarded: " + finalCompany.name + " | Applyra";

                // 1. Notify all super admins
                List<User> superAdmins = UserRepository.INSTANCE.findByUserType(UserType.SUPER_ADMIN);
                for (User sa : superAdmins) {
                    EmailService.sendEmail(sa.email, subject, notificationHtml).subscribe(
                            ok  -> System.out.println("[OnboardCompany] Notified superadmin: " + sa.email),
                            err -> System.err.println("[OnboardCompany] Failed to notify superadmin: " + err.getMessage())
                    );
                }

                // 2. Notify all college users (TPOs + admins) of the onboarding college
                List<User> collegeUsers = UserRepository.INSTANCE.byCollege(collegeId);
                for (User cu : collegeUsers) {
                    if (cu.email != null && !cu.email.equals(finalHrEmail)) {
                        EmailService.sendEmail(cu.email, subject, notificationHtml).subscribe(
                                ok  -> {},
                                err -> System.err.println("[OnboardCompany] Failed to notify college user: " + err.getMessage())
                        );
                    }
                }

                // 3. Send credentials to the new HR (if user was created)
                if (finalUserCreated && finalPassword != null) {
                    String credHtml = EmailService.buildCompanyCredentialsHtml(
                            finalCompany.name, collegeName, finalHrEmail, finalPassword
                    );
                    EmailService.sendEmail(finalHrEmail, "Your Applyra Company Login | " + collegeName, credHtml)
                            .subscribe(
                                    sent -> System.out.println("[OnboardCompany] Credentials " + (sent ? "sent" : "failed") + " to " + finalHrEmail),
                                    err  -> System.err.println("[OnboardCompany] Credential email error: " + err.getMessage())
                            );
                } else {
                    // Company HR already existed — still notify them of the new college link
                    String linkHtml = EmailService.buildCompanyOnboardingNotificationHtml(
                            finalCompany.name, finalCompany.code, finalCompany.industry,
                            finalCompany.headquarters, finalHrEmail,
                            collegeName + " has linked your company"
                    );
                    EmailService.sendEmail(finalHrEmail, "Your company is now linked to " + collegeName + " | Applyra", linkHtml)
                            .subscribe(ok -> {}, err -> {});
                }

            } catch (Exception e) {
                System.err.println("[OnboardCompany] Email thread error: " + e.getMessage());
            }
        }, "company-onboard-email").start();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", finalCompanyCreated
                ? "Company created and linked to your college. Credentials sent to " + hrEmail
                : "Existing company linked to your college");
        response.put("companyId",        company.getId());
        response.put("companyCode",      company.code);
        response.put("companyName",      company.getName());
        response.put("companyCollegeId", cc.getId());
        response.put("hrEmail",          hrEmail);
        response.put("userCreated",      userCreated);
        return response;
    }

    private static String str(JsonObject body, String key) {
        String v = body.getString(key);
        return (v != null && !v.isBlank()) ? v.trim() : null;
    }

    private String generatePassword(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }
}

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
 * Sends credentials email to the company HR contact.
 *
 * POST /college/companies/onboard
 * Body: { "name": "Google", "industry": "IT", "website": "https://google.com",
 *         "contactEmail": "hr@google.com", "contactPhone": "9876543210",
 *         "description": "...", "headquarters": "Bangalore" }
 */
@CollegeRole
public enum OnboardCompanyController implements BaseController {

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
        var body = request.getRequest();

        String name = body.get("name");
        String contactEmail = body.get("contactEmail");

        if (name == null || name.isBlank()) {
            throw new RoutingError("Company name is required");
        }
        if (contactEmail == null || contactEmail.isBlank()) {
            throw new RoutingError("Contact email is required");
        }

        final String hrEmail = contactEmail.trim().toLowerCase();
        final String companyName = name.trim();

        Long collegeId = request.getCollege().getId();
        String collegeName = request.getCollege().getName();

        // Check if company with same name already exists
        Company existingCompany = CompanyRepository.INSTANCE.byName(companyName);
        Company company;
        boolean companyCreated = false;

        if (existingCompany != null) {
            company = existingCompany;
            // Check if already linked to this college
            CompanyCollege existingLink = CompanyCollegeRepository.INSTANCE.byCompanyAndCollege(company.getId(), collegeId);
            if (existingLink != null) {
                throw new RoutingError("Company '" + companyName + "' is already linked to your college");
            }
        } else {
            // Create new company
            company = new Company();
            company.name = companyName;
            company.industry = body.get("industry") != null ? ((String) body.get("industry")).trim() : null;
            company.website = body.get("website") != null ? ((String) body.get("website")).trim() : null;
            company.contactEmail = hrEmail;
            company.contactPhone = body.get("contactPhone") != null ? ((String) body.get("contactPhone")).trim() : null;
            company.description = body.get("description") != null ? ((String) body.get("description")).trim() : null;
            company.headquarters = body.get("headquarters") != null ? ((String) body.get("headquarters")).trim() : null;
            company.active = true;
            company.save();
            int companySeq = CompanyRepository.INSTANCE.countAll();
            company.code = "CMP-" + request.getCollege().getCode() + "-" + String.format("%03d", companySeq);
            company.update();
            companyCreated = true;
        }

        // Create or find COMPANY_HR user for this email
        User hrUser = UserRepository.INSTANCE.byEmail(hrEmail);
        String rawPassword = null;
        boolean userCreated = false;

        if (hrUser == null) {
            rawPassword = generatePassword(12);
            hrUser = new User();
            hrUser.email = hrEmail;
            hrUser.name = companyName + " HR";
            hrUser.password = PasswordUtils.INSTANCE.hash(rawPassword);
            hrUser.userType = UserType.COMPANY_HR;
            hrUser.verified = true;
            hrUser.active = true;
            hrUser.save();
            userCreated = true;
        }

        // Create CompanyCollege link
        CompanyCollege cc = new CompanyCollege();
        cc.company = company;
        cc.college = request.getCollege();
        cc.managedByUser = hrUser;
        cc.companyCanManage = true;
        cc.active = true;
        cc.save();

        // Send credentials email if new user was created
        if (userCreated && rawPassword != null) {
            final String finalPassword = rawPassword;
            new Thread(() -> {
                try {
                    String html = EmailService.buildCompanyCredentialsHtml(
                            companyName, collegeName, hrEmail, finalPassword
                    );
                    EmailService.sendEmail(hrEmail, "Your Applyra Company Login — " + collegeName, html)
                            .subscribe(
                                    sent -> System.out.println("[OnboardCompany] Credentials " + (sent ? "sent" : "failed") + " to " + hrEmail),
                                    err -> System.err.println("[OnboardCompany] Email error: " + err.getMessage())
                            );
                } catch (Exception e) {
                    System.err.println("[OnboardCompany] Email thread error: " + e.getMessage());
                }
            }, "company-onboard-email").start();
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", companyCreated
                ? "Company created and linked to your college. Credentials sent to " + hrEmail
                : "Existing company linked to your college");
        response.put("companyId", company.getId());
        response.put("companyName", company.getName());
        response.put("companyCollegeId", cc.getId());
        response.put("hrEmail", hrEmail);
        response.put("userCreated", userCreated);
        return response;
    }

    private String generatePassword(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }
}

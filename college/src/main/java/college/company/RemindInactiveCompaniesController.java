package college.company;

import helpers.annotations.CollegeRole;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.college.CollegeAccessMiddleware;
import models.body.CollegeLoginRequest;
import models.repos.CompanyCollegeRepository;
import models.repos.DriveRepository;
import models.services.EmailService;
import models.sql.College;
import models.sql.CompanyCollege;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * POST /college/companies/remind-inactive
 * Sends a nudge email to companies that are linked to the college but have never run a drive.
 */
@CollegeRole
public enum RemindInactiveCompaniesController implements BaseController {

    INSTANCE;

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
        College college = request.getCollege();

        List<CompanyCollege> companyColleges = CompanyCollegeRepository.INSTANCE.byCollege(college.getId());

        List<Map<String, String>> nudgedCompanies = new ArrayList<>();

        for (CompanyCollege cc : companyColleges) {
            if (cc.getCompany() == null) continue;

            // Check if this company has any drives for this college
            int driveCount = DriveRepository.INSTANCE.where()
                    .eq("companyCollege.company.id", cc.getCompany().getId())
                    .eq("companyCollege.college.id", college.getId())
                    .findCount();

            if (driveCount > 0) continue; // Not inactive — skip

            String contactEmail = cc.getCompany().getContactEmail();
            if (contactEmail == null || contactEmail.isEmpty()) continue;

            String companyName = cc.getCompany().getName();

            String subject = "You're on Applyra – Let's run your first drive at " + college.getName() + "!";
            String html = "<p>Dear " + companyName + " Team,</p>"
                    + "<p>You have successfully onboarded on <strong>Applyra</strong>, the campus placement platform "
                    + "for <strong>" + college.getName() + "</strong>.</p>"
                    + "<p>However, we noticed that you have not yet participated in any campus placement drive at this college. "
                    + "We'd love to have you! There are talented students ready to contribute to your team.</p>"
                    + "<p>Log in to your Applyra dashboard to create a drive and start connecting with students.</p>"
                    + "<p>Best regards,<br/>Placement Cell<br/>" + college.getName() + "</p>";

            EmailService.sendEmail(contactEmail, subject, html)
                    .subscribe(ok -> {}, err -> System.err.println("[RemindInactiveCompanies] Email error: " + err.getMessage()));

            Map<String, String> info = new HashMap<>();
            info.put("name", companyName);
            info.put("email", contactEmail);
            nudgedCompanies.add(info);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("message", "Nudge sent to " + nudgedCompanies.size() + " companies");
        result.put("count", nudgedCompanies.size());
        result.put("companies", nudgedCompanies);
        return result;
    }
}

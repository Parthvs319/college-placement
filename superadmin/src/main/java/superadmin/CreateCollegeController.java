package superadmin;

import helpers.annotations.SuperAdminRole;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.ebean.DB;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.superadmin.SuperAdminAccessMiddleware;
import models.body.SuperAdminLoginRequest;
import models.json.CollegeDtos;
import models.repos.AisheCollegeRepository;
import models.repos.CollegeRepository;
import models.services.EmailService;
import models.sql.City;
import models.sql.College;
import models.sql.States;

import java.util.ArrayList;

@SuperAdminRole(request = {
        "name:string@required",
        "code:string@required",
        "cityId:long",
        "stateId:long",
        "university:string",
        "website:string",
        "gstin:string",
        "contactEmail:string",
        "contactPhone:string",
        "tpoName:string",
        "isEmailVerified:boolean",
        "isPhoneVerified:boolean",
        "aisheCode:string"
})
public enum CreateCollegeController implements BaseController {

    INSTANCE;

    @Override
    public void handle(RoutingContext event) {
        SuperAdminAccessMiddleware.INSTANCE.with(event, new ArrayList<>(), this.getClass())
                .map(this::map)
                .subscribe(
                        o -> ResponseUtils.INSTANCE.writeJsonResponse(event, o),
                        error -> ResponseUtils.INSTANCE.handleError(event, error)
                );
    }

    private Object map(SuperAdminLoginRequest request) {
        var body = request.getRequest();
        String name = body.get("name");
        String code = body.get("code");
        String contactEmail = body.get("contactEmail");

        if (name == null || name.isBlank()) {
            throw new RoutingError("College name is required");
        }
        if (code == null || code.isBlank()) {
            throw new RoutingError("College code is required");
        }
        if (CollegeRepository.INSTANCE.byCode(code.toUpperCase()) != null) {
            throw new RoutingError("College with code " + code.toUpperCase() + " already exists");
        }
        if (CollegeRepository.INSTANCE.byEmail(contactEmail.toUpperCase()) != null) {
            throw new RoutingError("College with code " + code.toUpperCase() + " already exists");
        }

        // Resolve cityId and stateId
        String cityIdStr = body.get("cityId");
        String stateIdStr = body.get("stateId");
        Long cityId = null;
        Long stateId = null;
        String cityName = null;
        String stateName = null;

        if (stateIdStr != null && !stateIdStr.isBlank()) {
            stateId = Long.parseLong(stateIdStr);
            States st = DB.find(States.class, stateId);
            if (st == null) throw new RoutingError("Invalid stateId");
            stateName = st.getName();
        }
        if (cityIdStr != null && !cityIdStr.isBlank()) {
            cityId = Long.parseLong(cityIdStr);
            City ct = DB.find(City.class, cityId);
            if (ct == null) throw new RoutingError("Invalid cityId");
            cityName = ct.getName();
        }

        College college = new College();
        college.setName(name);
        college.setCode(code.toUpperCase());
        college.setCityId(cityId);
        college.setStateId(stateId);
        college.setUniversity(body.get("university"));
        college.setWebsite(body.get("website"));
        String gstin = body.get("gstin");
        if (gstin != null && !gstin.isBlank()) college.setGstin(gstin.toUpperCase().trim());
        college.setContactEmail(contactEmail);
        college.setContactPhone(body.get("contactPhone"));
        college.setTpoName(body.get("tpoName"));
        college.setEmailVerified((body.get("isEmailVerified")));
        college.setPhoneVerified((body.get("isPhoneVerified")));

        // AISHE validation — store code if provided and valid
        String aisheCode = body.get("aisheCode");
        if (aisheCode != null && !aisheCode.isBlank()) {
            if (AisheCollegeRepository.INSTANCE.findByAisheCode(aisheCode.trim()) != null) {
                college.setAisheCode(aisheCode.trim());
            }
            // If code not found in DB we silently ignore it (don't block onboarding)
        }

        college.setVerified(false);
        college.setActive(false);
        college.save();

        // Send welcome email on a separate thread
        if (college.getContactEmail() != null && !college.getContactEmail().isBlank()) {
            final String fCityName = cityName;
            final String fStateName = stateName;
            new Thread(() -> {
                try {
                    String html = EmailService.buildCollegeOnboardingHtml(
                            college.getName(), college.getCode(), fCityName, fStateName, college.getWebsite()
                    );
                    EmailService.sendEmail(college.getContactEmail(), "Welcome to Applyra - " + college.getName(), html)
                            .subscribe(
                                    sent -> System.out.println("[CreateCollege] Welcome email " + (sent ? "sent" : "failed") + " to " + college.getContactEmail()),
                                    err -> System.err.println("[CreateCollege] Email error: " + err.getMessage())
                            );
                } catch (Exception e) {
                    System.err.println("[CreateCollege] Email thread error: " + e.getMessage());
                }
            }, "college-welcome-email").start();
        }

        return CollegeDtos.toCollegeDto(college);
    }
}

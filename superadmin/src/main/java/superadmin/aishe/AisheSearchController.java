package superadmin.aishe;

import superadmin.SuperAdminDtos;

import helpers.annotations.SuperAdminRole;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.superadmin.SuperAdminAccessMiddleware;
import models.repos.AisheCollegeRepository;
import models.sql.AisheCollege;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * GET /admin/aishe/search?q=&lt;query&gt;&amp;state=&lt;optional&gt;&amp;limit=&lt;optional, default 10, max 20&gt;
 *
 * Searches the aishe_colleges table by name (LIKE %query%). Optionally filtered by state.
 * Returns a list of AisheCollegeResult DTOs ordered by name.
 */
@SuperAdminRole
public enum AisheSearchController implements BaseController {

    INSTANCE;

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT      = 20;

    @Override
    public void handle(RoutingContext event) {
        SuperAdminAccessMiddleware.INSTANCE.with(event, new ArrayList<>(), this.getClass())
                .map(req -> search(event))
                .subscribe(
                        o -> ResponseUtils.INSTANCE.writeJsonResponse(event, o),
                        error -> ResponseUtils.INSTANCE.handleError(event, error)
                );
    }

    private List<SuperAdminDtos.AisheCollegeResult> search(RoutingContext rc) {
        String q     = rc.queryParam("q").stream().findFirst().orElse("").trim();
        String state = rc.queryParam("state").stream().findFirst().orElse("").trim();

        int limit = DEFAULT_LIMIT;
        String limitParam = rc.queryParam("limit").stream().findFirst().orElse("");
        if (!limitParam.isBlank()) {
            try {
                limit = Math.min(Integer.parseInt(limitParam), MAX_LIMIT);
            } catch (NumberFormatException ignored) {}
        }

        if (q.length() < 2) return new ArrayList<>();

        List<AisheCollege> results = AisheCollegeRepository.INSTANCE.search(
                q,
                state.isBlank() ? null : state,
                limit
        );

        return results.stream().map(this::toDto).collect(Collectors.toList());
    }

    private SuperAdminDtos.AisheCollegeResult toDto(AisheCollege c) {
        SuperAdminDtos.AisheCollegeResult dto = new SuperAdminDtos.AisheCollegeResult();
        dto.setId(c.getId());
        dto.setAisheCode(c.getAisheCode());
        dto.setName(c.getName());
        dto.setState(c.getState());
        dto.setDistrict(c.getDistrict());
        dto.setWebsite(c.getWebsite());
        dto.setYearOfEstablishment(c.getYearOfEstablishment());
        dto.setLocation(c.getLocation());
        dto.setCollegeType(c.getCollegeType());
        dto.setManagement(c.getManagement());
        dto.setUniversityAisheCode(c.getUniversityAisheCode());
        dto.setUniversityName(c.getUniversityName());
        dto.setUniversityType(c.getUniversityType());
        return dto;
    }
}

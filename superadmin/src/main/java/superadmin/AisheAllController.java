package superadmin;

import helpers.annotations.SuperAdminRole;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.superadmin.SuperAdminAccessMiddleware;
import models.repos.AisheCollegeRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * GET /admin/aishe/all
 *
 * Returns a slim list of all AISHE college records (aisheCode, name, state, district only).
 * Used by the frontend to build a Fuse.js in-memory index for instant fuzzy search.
 * Response is ~1MB gzipped for ~49k records — fetched once and cached client-side.
 */
@SuperAdminRole
public enum AisheAllController implements BaseController {

    INSTANCE;

    @Override
    public void handle(RoutingContext event) {
        SuperAdminAccessMiddleware.INSTANCE.with(event, new ArrayList<>(), this.getClass())
                .map(req -> {
                    List<SuperAdminDtos.AisheSlimRecord> records =
                            AisheCollegeRepository.INSTANCE.findAllSlim()
                                    .stream()
                                    .map(c -> {
                                        SuperAdminDtos.AisheSlimRecord r = new SuperAdminDtos.AisheSlimRecord();
                                        r.setAisheCode(c.getAisheCode());
                                        r.setName(c.getName());
                                        r.setState(c.getState());
                                        r.setDistrict(c.getDistrict());
                                        return r;
                                    })
                                    .collect(Collectors.toList());
                    return records;
                })
                .subscribe(
                        o -> ResponseUtils.INSTANCE.writeJsonResponse(event, o),
                        error -> ResponseUtils.INSTANCE.handleError(event, error)
                );
    }
}

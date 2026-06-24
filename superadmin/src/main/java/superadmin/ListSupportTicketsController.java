package superadmin;

import helpers.annotations.SuperAdminRole;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.superadmin.SuperAdminAccessMiddleware;
import models.body.SuperAdminLoginRequest;
import models.repos.SupportTicketRepository;
import models.sql.SupportTicket;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Super admin views all support tickets.
 *
 * GET /admin/support/tickets?status=OPEN&priority=P1
 */
@SuperAdminRole
public enum ListSupportTicketsController implements BaseController {

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
        String statusFilter   = request.getRoutingContext().queryParams().get("status");
        String priorityFilter = request.getRoutingContext().queryParams().get("priority");

        List<SupportTicket> tickets;
        if (statusFilter != null && !statusFilter.isEmpty()) {
            tickets = SupportTicketRepository.INSTANCE.byStatus(statusFilter.toUpperCase());
        } else {
            tickets = SupportTicketRepository.INSTANCE.all();
        }

        if (priorityFilter != null && !priorityFilter.isEmpty()) {
            final String pf = priorityFilter.toUpperCase();
            tickets = tickets.stream().filter(t -> pf.equals(t.getPriority())).collect(Collectors.toList());
        }

        List<Map<String, Object>> dtos = tickets.stream().map(this::toDto).collect(Collectors.toList());

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("tickets", dtos);
        res.put("total", dtos.size());
        return res;
    }

    private Map<String, Object> toDto(SupportTicket t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId());
        m.put("subject", t.getSubject());
        m.put("description", t.getDescription());
        m.put("priority", t.getPriority());
        m.put("status", t.getStatus());
        m.put("notes", t.getNotes());
        m.put("raisedByName",  t.getRaisedBy() != null ? t.getRaisedBy().getName()  : null);
        m.put("raisedByEmail", t.getRaisedBy() != null ? t.getRaisedBy().getEmail() : null);
        m.put("collegeName",   t.getCollege()  != null ? t.getCollege().getName()   : null);
        m.put("collegeCode",   t.getCollege()  != null ? t.getCollege().getCode()   : null);
        m.put("createdAt",  t.getCreatedAt()  != null ? t.getCreatedAt().toString()  : null);
        m.put("resolvedAt", t.getResolvedAt() != null ? t.getResolvedAt().toString() : null);
        return m;
    }
}

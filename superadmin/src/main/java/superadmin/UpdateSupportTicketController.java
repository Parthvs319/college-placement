package superadmin;

import helpers.annotations.SuperAdminRole;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.superadmin.SuperAdminAccessMiddleware;
import models.body.SuperAdminLoginRequest;
import models.repos.SupportTicketRepository;
import models.sql.SupportTicket;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Super admin updates a support ticket's status or notes.
 *
 * PUT /admin/support/tickets/:ticketId
 * Body: { "status": "IN_PROGRESS" | "RESOLVED", "notes": "..." }
 */
@SuperAdminRole
public enum UpdateSupportTicketController implements BaseController {

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
        String ticketIdStr = request.getRoutingContext().pathParam("ticketId");
        if (ticketIdStr == null) throw new RoutingError("ticketId path param is required");

        long ticketId;
        try { ticketId = Long.parseLong(ticketIdStr); }
        catch (NumberFormatException e) { throw new RoutingError("Invalid ticketId"); }

        SupportTicket ticket = SupportTicketRepository.INSTANCE.byId(ticketId);
        if (ticket == null) throw new RoutingError(404, "Ticket not found");

        if (request.getRequest().isPresent("status")) {
            String newStatus = request.getRequest().get("status");
            if (!newStatus.equals("OPEN") && !newStatus.equals("IN_PROGRESS") && !newStatus.equals("RESOLVED")) {
                throw new RoutingError("status must be OPEN, IN_PROGRESS, or RESOLVED");
            }
            ticket.setStatus(newStatus);
            if ("RESOLVED".equals(newStatus) && ticket.getResolvedAt() == null) {
                ticket.setResolvedAt(Timestamp.from(Instant.now()));
            }
        }

        if (request.getRequest().isPresent("notes")) {
            ticket.setNotes(request.getRequest().get("notes"));
        }

        ticket.save();

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", ticket.getId());
        m.put("subject", ticket.getSubject());
        m.put("priority", ticket.getPriority());
        m.put("status", ticket.getStatus());
        m.put("notes", ticket.getNotes());
        m.put("resolvedAt", ticket.getResolvedAt() != null ? ticket.getResolvedAt().toString() : null);
        m.put("message", "Ticket updated");
        return m;
    }
}

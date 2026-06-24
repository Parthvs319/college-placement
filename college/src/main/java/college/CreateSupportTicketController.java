package college;

import helpers.annotations.CollegeRole;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.college.CollegeAccessMiddleware;
import models.body.CollegeLoginRequest;
import models.repos.SupportTicketRepository;
import models.sql.SupportTicket;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * TPO / College Admin raises a support ticket.
 *
 * POST /college/support/tickets
 * Body: { "subject": "...", "description": "...", "priority": "P1" | "P2" }
 */
@CollegeRole
public enum CreateSupportTicketController implements BaseController {

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
        String subject = request.getRequest().get("subject");
        if (subject == null || subject.trim().isEmpty()) throw new RoutingError("subject is required");

        String description = request.getRequest().get("description");
        if (description == null || description.trim().isEmpty()) throw new RoutingError("description is required");

        String priority = request.getRequest().isPresent("priority") ? request.getRequest().get("priority") : "P2";
        if (!priority.equals("P1") && !priority.equals("P2")) {
            throw new RoutingError("priority must be P1 or P2");
        }

        SupportTicket ticket = new SupportTicket();
        ticket.setRaisedBy(request.getUser());
        ticket.setCollege(request.getCollege());
        ticket.setSubject(subject.trim());
        ticket.setDescription(description.trim());
        ticket.setPriority(priority);
        ticket.setStatus("OPEN");

        ticket.save();

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("id", ticket.getId());
        res.put("subject", ticket.getSubject());
        res.put("priority", ticket.getPriority());
        res.put("status", ticket.getStatus());
        res.put("createdAt", ticket.getCreatedAt() != null ? ticket.getCreatedAt().toString() : null);
        res.put("message", "Ticket #" + ticket.getId() + " raised successfully. Our team will respond shortly.");
        return res;
    }
}

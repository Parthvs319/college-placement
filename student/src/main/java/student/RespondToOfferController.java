package student;

import helpers.annotations.StudentRole;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.Request;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.student.StudentAccessMiddleware;
import models.body.StudentLoginRequest;
import models.enums.OfferStatus;
import models.repos.OfferRepository;
import models.repos.PlacementPolicyRepository;
import models.repos.StudentRepository;
import models.sql.Offer;
import models.sql.PlacementPolicy;
import models.sql.Student;

import java.sql.Timestamp;
import java.util.ArrayList;

/**
 * Student accepts or declines an offer.
 * Enforces placement policy: dream CTC, block after first accept, max simultaneous offers.
 */
@StudentRole
public enum RespondToOfferController implements BaseController {

    INSTANCE;

    @Override
    public void handle(RoutingContext event) {
        StudentAccessMiddleware.INSTANCE.with(event, new ArrayList<>(), this.getClass())
                .map(this::map)
                .subscribe(
                        o -> ResponseUtils.INSTANCE.writeJsonResponse(event, o),
                        error -> ResponseUtils.INSTANCE.handleError(event, error)
                );
    }

    private Object map(StudentLoginRequest request) {
        Student student = StudentRepository.INSTANCE.byUserId(request.getUser().getId());
        if (student == null) {
            throw new RoutingError("Student profile not found");
        }

        String offerIdParam = request.getRoutingContext().pathParam("offerId");
        Offer offer = OfferRepository.INSTANCE.byId(Long.parseLong(offerIdParam));
        if (offer == null) {
            throw new RoutingError("Offer not found");
        }

        if (!offer.student.getId().equals(student.getId())) {
            throw new RoutingError("This offer does not belong to you");
        }

        if (!offer.status.equals(OfferStatus.PENDING)) {
            throw new RoutingError("This offer has already been " + offer.status.name().toLowerCase());
        }

        Request body = request.getRequest();
        String action = body.get("action"); // "accept" or "decline"
        if (action == null || (!action.equals("accept") && !action.equals("decline"))) {
            throw new RoutingError("action must be 'accept' or 'decline'");
        }

        if (action.equals("accept")) {
            // Enforce placement policy
            PlacementPolicy policy = PlacementPolicyRepository.INSTANCE.latestByCollege(student.college.getId());
            if (policy != null) {
                // Check block after first accept
                if (policy.blockAfterFirstAccept) {
                    var accepted = OfferRepository.INSTANCE.acceptedByStudent(student.getId());
                    if (!accepted.isEmpty()) {
                        throw new RoutingError("You have already accepted an offer. Policy does not allow accepting more.");
                    }
                }

                // Check max simultaneous offers
                int activeCount = OfferRepository.INSTANCE.countActiveByStudent(student.getId());
                if (activeCount >= policy.maxSimultaneousOffers) {
                    throw new RoutingError("You have reached the maximum number of active offers (" + policy.maxSimultaneousOffers + ")");
                }
            }

            offer.status = OfferStatus.ACCEPTED;
            offer.respondedAt = new Timestamp(System.currentTimeMillis());
            offer.update();

            // Update student placement status
            student.placed = true;
            student.placedAt = new Timestamp(System.currentTimeMillis());
            student.currentCtc = offer.ctcOffered;
            student.update();

            // Check dream CTC — if accepted offer >= threshold, block further participation
            if (policy != null && policy.dreamCtcThreshold != null
                    && offer.ctcOffered.compareTo(policy.dreamCtcThreshold) >= 0) {
                student.optedOut = true;
                student.update();
            }
        } else {
            offer.status = OfferStatus.DECLINED;
            offer.respondedAt = new Timestamp(System.currentTimeMillis());
            offer.update();
        }

        return StudentDtos.toOfferDto(offer);
    }
}

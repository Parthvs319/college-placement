package superadmin;

import helpers.annotations.SuperAdminRole;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.superadmin.SuperAdminAccessMiddleware;
import models.repos.*;
import models.sql.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@SuperAdminRole
public enum PlatformAnalyticsController implements BaseController {

    INSTANCE;

    @Override
    public void handle(RoutingContext event) {
        SuperAdminAccessMiddleware.INSTANCE.with(event, new ArrayList<>(), this.getClass())
                .map(req -> {
                    SuperAdminDtos.PlatformAnalytics a = new SuperAdminDtos.PlatformAnalytics();

                    List<College> colleges = CollegeRepository.INSTANCE.findAll();
                    a.setTotalColleges(colleges.size());
                    a.setActiveColleges((int) colleges.stream().filter(c -> c.active).count());

                    List<User> users = UserRepository.INSTANCE.findAll();
                    a.setTotalUsers(users.size());

                    // Aggregate students across all colleges
                    int totalStudents = 0;
                    int placedStudents = 0;
                    for (College c : colleges) {
                        List<Student> students = StudentRepository.INSTANCE.byCollege(c.getId());
                        totalStudents += students.size();
                        placedStudents += StudentRepository.INSTANCE.findPlaced(c.getId()).size();
                    }
                    a.setTotalStudents(totalStudents);
                    a.setPlacedStudents(placedStudents);
                    a.setUnplacedStudents(totalStudents - placedStudents);

                    List<Company> allCompanies = CompanyRepository.INSTANCE.findAll();
                    a.setTotalCompanies(allCompanies.size());
                    a.setStartupCount((int) allCompanies.stream().filter(c -> c.startup).count());

                    List<Drive> allDrives = DriveRepository.INSTANCE.where().findList();
                    a.setTotalDrives(allDrives.size());
                    a.setActiveDrives((int) allDrives.stream()
                            .filter(d -> d.status != null
                                    && !d.status.name().equals("COMPLETED")
                                    && !d.status.name().equals("CANCELLED"))
                            .count());

                    List<Offer> allOffers = OfferRepository.INSTANCE.where().findList();
                    a.setTotalOffers(allOffers.size());

                    if (totalStudents > 0) {
                        a.setOverallPlacementRate(
                                (double) placedStudents / totalStudents * 100
                        );
                    }

                    // CTC stats from offers (use offer-level ctcOffered directly)
                    BigDecimal totalCtc = BigDecimal.ZERO;
                    BigDecimal highest = BigDecimal.ZERO;
                    BigDecimal lowest = null;
                    int ctcCount = 0;
                    for (Offer o : allOffers) {
                        BigDecimal ctc = o.ctcOffered;
                        if (ctc == null && o.getDrive() != null) {
                            ctc = o.getDrive().ctcOffered;
                        }
                        if (ctc != null && ctc.compareTo(BigDecimal.ZERO) > 0) {
                            totalCtc = totalCtc.add(ctc);
                            ctcCount++;
                            if (ctc.compareTo(highest) > 0) {
                                highest = ctc;
                            }
                            if (lowest == null || ctc.compareTo(lowest) < 0) {
                                lowest = ctc;
                            }
                        }
                    }
                    a.setHighestCtc(highest);
                    a.setLowestCtc(lowest != null ? lowest : BigDecimal.ZERO);
                    if (ctcCount > 0) {
                        a.setAverageCtc(totalCtc.divide(BigDecimal.valueOf(ctcCount), 2, RoundingMode.HALF_UP));
                    }

                    return a;
                })
                .subscribe(
                        o -> ResponseUtils.INSTANCE.writeJsonResponse(event, o),
                        error -> ResponseUtils.INSTANCE.handleError(event, error)
                );
    }
}

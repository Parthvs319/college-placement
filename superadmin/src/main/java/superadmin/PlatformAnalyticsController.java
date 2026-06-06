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

                    a.setTotalCompanies(CompanyRepository.INSTANCE.findAll().size());

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

                    // CTC stats from offers
                    BigDecimal totalCtc = BigDecimal.ZERO;
                    BigDecimal highest = BigDecimal.ZERO;
                    int ctcCount = 0;
                    for (Offer o : allOffers) {
                        if (o.getDrive() != null && o.getDrive().ctcOffered != null) {
                            BigDecimal ctc = o.getDrive().ctcOffered;
                            totalCtc = totalCtc.add(ctc);
                            ctcCount++;
                            if (ctc.compareTo(highest) > 0) {
                                highest = ctc;
                            }
                        }
                    }
                    a.setHighestCtc(highest);
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

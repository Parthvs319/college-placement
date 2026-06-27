package superadmin.analytics;

import superadmin.SuperAdminDtos;

import helpers.annotations.SuperAdminRole;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.superadmin.SuperAdminAccessMiddleware;
import models.repos.*;
import models.sql.*;

import java.util.ArrayList;
import java.util.List;

@SuperAdminRole
public enum PlatformAnalyticsController implements BaseController {

    INSTANCE;

    @Override
    public void handle(RoutingContext event) {
        SuperAdminAccessMiddleware.INSTANCE.with(event, new ArrayList<>(), this.getClass())
                .map(req -> {
                    String yearParam = event.request().getParam("academicYear");
                    Integer academicYear = null;
                    if (yearParam != null && !yearParam.isEmpty()) {
                        academicYear = Integer.parseInt(yearParam);
                    }

                    SuperAdminDtos.PlatformAnalytics a = new SuperAdminDtos.PlatformAnalytics();

                    List<College> colleges = CollegeRepository.INSTANCE.findAll();
                    a.setTotalColleges(colleges.size());
                    a.setActiveColleges((int) colleges.stream().filter(c -> c.isActive()).count());

                    List<User> users = UserRepository.INSTANCE.findAll();
                    a.setTotalUsers(users.size());

                    // Aggregate students across all colleges
                    int totalStudents  = 0;
                    int placedStudents = 0;
                    int internshipCount = 0;
                    int ppoCount        = 0;
                    for (College c : colleges) {
                        List<Student> students = StudentRepository.INSTANCE.byCollege(c.getId());
                        totalStudents   += students.size();
                        placedStudents  += StudentRepository.INSTANCE.findPlaced(c.getId()).size();
                        internshipCount += (int) students.stream().filter(Student::isInternship).count();
                        ppoCount        += (int) students.stream().filter(Student::isPpo).count();
                    }
                    a.setTotalStudents(totalStudents);
                    a.setPlacedStudents(placedStudents);
                    a.setUnplacedStudents(totalStudents - placedStudents);
                    a.setInternshipCount(internshipCount);
                    a.setPpoCount(ppoCount);

                    List<Company> allCompanies = CompanyRepository.INSTANCE.findAll();
                    a.setTotalCompanies(allCompanies.size());
                    a.setStartupCount((int) allCompanies.stream().filter(c -> c.isStartup()).count());

                    var driveQuery = DriveRepository.INSTANCE.where();
                    if (academicYear != null) {
                        driveQuery.eq("academicYear", academicYear);
                    }
                    List<Drive> allDrives = driveQuery.findList();
                    a.setTotalDrives(allDrives.size());
                    a.setActiveDrives((int) allDrives.stream()
                            .filter(d -> d.getStatus() != null
                                    && !d.getStatus().name().equals("COMPLETED")
                                    && !d.getStatus().name().equals("CANCELLED"))
                            .count());

                    var offerQuery = OfferRepository.INSTANCE.where();
                    if (academicYear != null) {
                        offerQuery.eq("drive.academicYear", academicYear);
                    }
                    List<Offer> allOffers = offerQuery.findList();
                    a.setTotalOffers(allOffers.size());

                    if (totalStudents > 0) {
                        a.setOverallPlacementRate(
                                (double) placedStudents / totalStudents * 100
                        );
                    }

                    return a;
                })
                .subscribe(
                        o -> ResponseUtils.INSTANCE.writeJsonResponse(event, o),
                        error -> ResponseUtils.INSTANCE.handleError(event, error)
                );
    }
}

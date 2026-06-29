package college;

import helpers.annotations.CollegeRole;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import lombok.Data;
import models.access.middlewear.college.CollegeAccessMiddleware;
import models.body.CollegeLoginRequest;
import models.enums.EmploymentType;
import models.repos.*;
import models.sql.Student;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@CollegeRole
public enum CollegeAnalyticsController implements BaseController {

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
        Long collegeId = request.getCollege().getId();

        List<Student> allStudents = StudentRepository.INSTANCE.byCollege(collegeId);
        List<Student> placed      = StudentRepository.INSTANCE.findPlaced(collegeId);
        List<Student> unplaced    = StudentRepository.INSTANCE.findUnplaced(collegeId);

        Analytics analytics = new Analytics();
        analytics.totalStudents    = allStudents.size();
        analytics.placedStudents   = placed.size();
        analytics.unplacedStudents = unplaced.size();
        analytics.totalCompanies   = CompanyCollegeRepository.INSTANCE.byCollege(collegeId).size();

        if (analytics.totalStudents > 0) {
            analytics.placementRate = (double) analytics.placedStudents / analytics.totalStudents * 100;
        }

        // Student-level internship / PPO / full-time counts
        analytics.internshipCount = (int) allStudents.stream().filter(Student::isInternship).count();
        analytics.ppoCount        = (int) allStudents.stream().filter(Student::isPpo).count();
        analytics.fullTimeCount   = analytics.placedStudents - analytics.ppoCount;
        if (analytics.fullTimeCount < 0) analytics.fullTimeCount = 0;

        // Drive counts by type
        analytics.fullTimeDrives   = DriveRepository.INSTANCE.countByCollegeAndType(collegeId, EmploymentType.FULL_TIME);
        analytics.internshipDrives = DriveRepository.INSTANCE.countByCollegeAndType(collegeId, EmploymentType.INTERNSHIP);
        analytics.ppoDrives        = DriveRepository.INSTANCE.countByCollegeAndType(collegeId, EmploymentType.PPO);

        // Total offers released
        analytics.totalOffers = OfferRepository.INSTANCE.countByCollege(collegeId);

        // Department-wise stats
        Map<String, int[]> deptMap = new LinkedHashMap<>();
        for (Student s : allStudents) {
            String dept = s.getDepartment() != null ? s.getDepartment() : "Other";
            deptMap.computeIfAbsent(dept, k -> new int[]{0, 0})[0]++;
        }
        for (Student s : placed) {
            String dept = s.getDepartment() != null ? s.getDepartment() : "Other";
            int[] arr = deptMap.computeIfAbsent(dept, k -> new int[]{0, 0});
            arr[1]++;
        }
        analytics.departmentStats = deptMap.entrySet().stream()
                .map(e -> {
                    DeptStat ds = new DeptStat();
                    ds.department = e.getKey();
                    ds.total      = e.getValue()[0];
                    ds.placed     = e.getValue()[1];
                    ds.unplaced   = ds.total - ds.placed;
                    ds.placementRate = ds.total > 0
                            ? Math.round((double) ds.placed / ds.total * 1000) / 10.0
                            : 0.0;
                    return ds;
                })
                .sorted((a, b) -> Integer.compare(b.total, a.total))
                .collect(Collectors.toList());

        return analytics;
    }

    @Data
    static class Analytics {
        int totalStudents;
        int placedStudents;
        int unplacedStudents;
        int totalCompanies;
        double placementRate;
        // Student-level counts
        int internshipCount;
        int ppoCount;
        int fullTimeCount;
        // Drive counts by type
        int fullTimeDrives;
        int internshipDrives;
        int ppoDrives;
        // Offers
        int totalOffers;
        List<DeptStat> departmentStats;
    }

    @Data
    static class DeptStat {
        String department;
        int total;
        int placed;
        int unplaced;
        double placementRate;
    }
}

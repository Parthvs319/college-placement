package superadmin;

import helpers.annotations.SuperAdminRole;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.superadmin.SuperAdminAccessMiddleware;
import models.enums.EmploymentType;
import models.enums.OfferStatus;
import models.enums.UserType;
import models.repos.*;
import models.sql.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@SuperAdminRole
public enum ListAllUsersController implements BaseController {

    INSTANCE;

    @Override
    public void handle(RoutingContext event) {
        SuperAdminAccessMiddleware.INSTANCE.with(event, new ArrayList<>(), this.getClass())
                .map(req -> {
                    String typeParam = event.request().getParam("type");
                    String collegeIdParam = event.request().getParam("collegeId");

                    List<User> users;
                    if (collegeIdParam != null && !collegeIdParam.isEmpty()) {
                        Long cId = Long.parseLong(collegeIdParam);
                        if (typeParam != null && !typeParam.isEmpty()) {
                            users = UserRepository.INSTANCE.byCollegeAndType(cId, UserType.valueOf(typeParam));
                        } else {
                            users = UserRepository.INSTANCE.byCollege(cId);
                        }
                    } else {
                        users = UserRepository.INSTANCE.findAll();
                        if (typeParam != null && !typeParam.isEmpty()) {
                            UserType filterType = UserType.valueOf(typeParam);
                            users = users.stream().filter(u -> u.getUserType() == filterType).collect(Collectors.toList());
                        }
                    }

                    return users.stream().map(u -> {
                        SuperAdminDtos.UserSummary s = new SuperAdminDtos.UserSummary();
                        s.setId(u.getId());
                        s.setName(u.getName());
                        s.setEmail(u.getEmail());
                        s.setMobile(u.getMobile());
                        s.setUserType(u.getUserType().getValue());
                        s.setVerified(u.isVerified());
                        s.setActive(u.isActive());
                        if (u.getCreatedAt() != null) {
                            s.setCreatedAt(u.getCreatedAt().toString());
                        }
                        if (u.getCollege() != null) {
                            s.setCollegeName(u.getCollege().getName());
                            s.setCollegeId(u.getCollege().getId());
                        }

                        // Student-specific enrichment
                        if (u.getUserType() == UserType.STUDENT) {
                            Student st = StudentRepository.INSTANCE.byUserId(u.getId());
                            if (st != null) {
                                s.setStudentId(st.getId());
                                s.setPlaced(st.isPlaced());
                                s.setDepartment(st.getDepartment());
                                s.setPassingYear(st.getPassingYear());
                                s.setCgpa(st.getCgpa());
                                if (st.getCurrentCtc() != null) {
                                    s.setCurrentCtc(st.getCurrentCtc());
                                }
                                // Find the company that placed the student (accepted offer)
                                if (st.isPlaced()) {
                                    List<Offer> accepted = OfferRepository.INSTANCE.acceptedByStudent(st.getId());
                                    if (!accepted.isEmpty()) {
                                        Offer placedOffer = accepted.get(0);
                                        if (placedOffer.getDrive() != null
                                                && placedOffer.getDrive().getCompanyCollege() != null
                                                && placedOffer.getDrive().getCompanyCollege().getCompany() != null) {
                                            s.setPlacedCompanyName(placedOffer.getDrive().getCompanyCollege().getCompany().getName());
                                        }
                                    }
                                }
                            }
                        }

                        // Company HR enrichment
                        if (u.getUserType() == UserType.COMPANY_HR) {
                            if (u.getCompany() != null) {
                                s.setCompanyId(u.getCompany().getId());
                                s.setCompanyName(u.getCompany().getName());

                                // Colleges associated via CompanyCollege
                                List<CompanyCollege> ccList = CompanyCollegeRepository.INSTANCE.byCompany(u.getCompany().getId());
                                s.setCollegeAssociatedCount(ccList.size());

                                // Drives by this company
                                List<Drive> companyDrives = DriveRepository.INSTANCE.byCompany(u.getCompany().getId());
                                s.setHrDriveCount(companyDrives.size());

                                // Offers across all drives - get CTC stats and total picked
                                BigDecimal highCtc = BigDecimal.ZERO;
                                BigDecimal lowCtc = null;
                                int totalPicked = 0;
                                for (Drive d : companyDrives) {
                                    List<Offer> offers = OfferRepository.INSTANCE.byDrive(d.getId());
                                    for (Offer o : offers) {
                                        if (o.getStatus() == OfferStatus.ACCEPTED) {
                                            totalPicked++;
                                        }
                                        BigDecimal ctc = o.getCtcOffered();
                                        if (ctc != null) {
                                            if (ctc.compareTo(highCtc) > 0) highCtc = ctc;
                                            if (lowCtc == null || ctc.compareTo(lowCtc) < 0) lowCtc = ctc;
                                        }
                                    }
                                }
                                s.setTotalStudentsPicked(totalPicked);
                                s.setHrHighestCtc(highCtc);
                                s.setHrLowestCtc(lowCtc);
                            } else {
                                // Fallback: check company_colleges managed_by_user_id
                                List<CompanyCollege> managed = CompanyCollegeRepository.INSTANCE.byManagedUser(u.getId());
                                if (!managed.isEmpty()) {
                                    String names = managed.stream()
                                            .filter(cc -> cc.getCompany() != null)
                                            .map(cc -> cc.getCompany().getName())
                                            .distinct()
                                            .collect(Collectors.joining(", "));
                                    if (!names.isEmpty()) s.setCompanyName(names);
                                    s.setCollegeAssociatedCount(managed.size());
                                }
                            }
                        }

                        // TPO / College Admin enrichment
                        if ((u.getUserType() == UserType.TPO || u.getUserType() == UserType.COLLEGE_ADMIN)
                                && u.getCollege() != null) {
                            Long cid = u.getCollege().getId();

                            List<Student> allStudents = StudentRepository.INSTANCE.byCollege(cid);
                            List<Student> placedStudents = StudentRepository.INSTANCE.findPlaced(cid);
                            s.setTotalFinalYearStudents(allStudents.size());
                            s.setTotalPlaced(placedStudents.size());
                            s.setTotalUnplaced(allStudents.size() - placedStudents.size());

                            // Companies onboarded
                            List<CompanyCollege> collegeCompanies = CompanyCollegeRepository.INSTANCE.byCollege(cid);
                            s.setTotalCompaniesOnboarded(collegeCompanies.size());

                            // Drives for CTC and employment type stats
                            List<Drive> drives = DriveRepository.INSTANCE.byCollege(cid);
                            BigDecimal highCtc = BigDecimal.ZERO;
                            BigDecimal lowCtc = null;
                            int internships = 0;
                            int fullTime = 0;

                            for (Drive d : drives) {
                                BigDecimal ctc = d.getCtcOffered();
                                if (ctc != null && ctc.compareTo(BigDecimal.ZERO) > 0) {
                                    if (ctc.compareTo(highCtc) > 0) highCtc = ctc;
                                    if (lowCtc == null || ctc.compareTo(lowCtc) < 0) lowCtc = ctc;
                                }
                                // Count offers by employment type
                                List<Offer> dOffers = OfferRepository.INSTANCE.byDrive(d.getId());
                                int acceptedInDrive = (int) dOffers.stream()
                                        .filter(o -> o.getStatus() == OfferStatus.ACCEPTED)
                                        .count();
                                if (d.getEmploymentType() == EmploymentType.INTERNSHIP) {
                                    internships += acceptedInDrive;
                                } else if (d.getEmploymentType() == EmploymentType.FULL_TIME
                                        || d.getEmploymentType() == EmploymentType.INTERN_PLUS_FTE) {
                                    fullTime += acceptedInDrive;
                                }
                            }

                            s.setTpoHighestCtc(highCtc);
                            s.setTpoLowestCtc(lowCtc);
                            s.setInternshipCount(internships);
                            s.setFullTimeOfferCount(fullTime);
                        }

                        return s;
                    }).collect(Collectors.toList());
                })
                .subscribe(
                        o -> ResponseUtils.INSTANCE.writeJsonResponse(event, o),
                        error -> ResponseUtils.INSTANCE.handleError(event, error)
                );
    }
}

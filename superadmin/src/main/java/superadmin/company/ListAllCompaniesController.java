package superadmin.company;

import superadmin.SuperAdminDtos;

import helpers.annotations.SuperAdminRole;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.superadmin.SuperAdminAccessMiddleware;
import models.repos.CompanyCollegeRepository;
import models.repos.CompanyRepository;
import models.repos.DriveRepository;
import models.repos.OfferRepository;
import models.sql.Company;
import models.sql.Drive;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@SuperAdminRole
public enum ListAllCompaniesController implements BaseController {

    INSTANCE;

    @Override
    public void handle(RoutingContext event) {
        SuperAdminAccessMiddleware.INSTANCE.with(event, new ArrayList<>(), this.getClass())
                .map(req -> {
                    List<Company> companies = CompanyRepository.INSTANCE.findAll();
                    return companies.stream().map(c -> {
                        SuperAdminDtos.CompanySummary s = new SuperAdminDtos.CompanySummary();
                        s.setId(c.getId());
                        s.setName(c.getName());
                        s.setCode(c.getCode());
                        s.setIndustry(c.getIndustry());
                        s.setWebsite(c.getWebsite());
                        s.setStartup(c.isStartup());
                        s.setCollegeCount(CompanyCollegeRepository.INSTANCE.byCompany(c.getId()).size());

                        List<Drive> drives = DriveRepository.INSTANCE.byCompany(c.getId());
                        s.setDriveCount(drives.size());

                        int totalOffers = 0;
                        for (Drive d : drives) {
                            totalOffers += OfferRepository.INSTANCE.byDrive(d.getId()).size();
                        }
                        s.setTotalOffers(totalOffers);
                        return s;
                    }).collect(Collectors.toList());
                })
                .subscribe(
                        o -> ResponseUtils.INSTANCE.writeJsonResponse(event, o),
                        error -> ResponseUtils.INSTANCE.handleError(event, error)
                );
    }
}

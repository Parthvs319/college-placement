package models.repos;

import helpers.sql.SqlFinder;
import models.sql.CompanyDocument;

import java.util.List;

public enum CompanyDocumentRepository {
    INSTANCE;

    private final SqlFinder<Long, CompanyDocument> finder = new SqlFinder<>(CompanyDocument.class);

    public CompanyDocument byId(Long id) {
        return finder.query().where().eq("id", id).eq("deleted", false).findOne();
    }

    public List<CompanyDocument> byCompanyId(Long companyId) {
        return finder.query().where()
                .eq("company.id", companyId)
                .eq("deleted", false)
                .orderBy("documentType asc, createdAt desc")
                .findList();
    }

    public List<CompanyDocument> byCompanyIdAndType(Long companyId, String documentType) {
        return finder.query().where()
                .eq("company.id", companyId)
                .eq("documentType", documentType)
                .eq("deleted", false)
                .orderBy("createdAt desc")
                .findList();
    }
}

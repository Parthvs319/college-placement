package models.repos;

import helpers.sql.SqlFinder;
import models.sql.CollegeInvoice;

import java.util.List;

public enum CollegeInvoiceRepository {
    INSTANCE;

    private final SqlFinder<Long, CollegeInvoice> finder = new SqlFinder<>(CollegeInvoice.class);

    public CollegeInvoice byId(Long id) {
        return finder.query().where().eq("id", id).eq("deleted", false).findOne();
    }

    public List<CollegeInvoice> byCollege(Long collegeId) {
        return finder.query().where()
                .eq("college.id", collegeId)
                .eq("deleted", false)
                .orderBy("createdAt desc")
                .findList();
    }

    public CollegeInvoice byInvoiceNumber(String invoiceNumber) {
        return finder.query().where()
                .eq("invoiceNumber", invoiceNumber)
                .eq("deleted", false)
                .findOne();
    }

    /** Count invoices for a college — used to generate sequential invoice numbers */
    public int countByCollege(Long collegeId) {
        return finder.query().where()
                .eq("college.id", collegeId)
                .eq("deleted", false)
                .findCount();
    }
}

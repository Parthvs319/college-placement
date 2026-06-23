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

    /**
     * Returns true if a non-cancelled invoice already exists for the given contract
     * whose billing period overlaps the given [start, end] range.
     * Used by GenerateInvoiceController to prevent duplicate invoices on the same contract.
     */
    public boolean existsForContractAndPeriod(Long contractId, String billingPeriodStart, String billingPeriodEnd) {
        return finder.query().where()
                .eq("contract.id", contractId)
                .ne("status", "CANCELLED")
                .eq("deleted", false)
                // Invoice period overlaps [start, end]:  inv.start <= end AND inv.end >= start
                .le("billingPeriodStart", billingPeriodEnd)
                .ge("billingPeriodEnd",   billingPeriodStart)
                .findCount() > 0;
    }

    /**
     * Returns true if a non-cancelled invoice already exists for the given college
     * whose billing period overlaps the given [start, end] range.
     * Used by InvoiceDueController to hide colleges that already have an invoice for this cycle,
     * regardless of which contract the invoice was raised against.
     */
    public boolean existsForCollegeAndPeriod(Long collegeId, String billingPeriodStart, String billingPeriodEnd) {
        return finder.query().where()
                .eq("college.id", collegeId)
                .ne("status", "CANCELLED")
                .eq("deleted", false)
                .le("billingPeriodStart", billingPeriodEnd)
                .ge("billingPeriodEnd",   billingPeriodStart)
                .findCount() > 0;
    }
}

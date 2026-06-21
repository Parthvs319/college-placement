package models.services;

import com.lowagie.text.*;
import com.lowagie.text.pdf.*;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Generates branded invoice PDFs using OpenPDF (LGPL).
 * Produces a clean, professional A4 invoice for each college billing cycle.
 */
public class InvoiceService {

    private static final Color BRAND_BLUE   = new Color(26, 115, 232);   // #1A73E8
    private static final Color BRAND_GOLD   = new Color(232, 152, 48);   // #E89830
    private static final Color TEXT_DARK    = new Color(26, 26, 26);     // #1A1A1A
    private static final Color TEXT_MUTED   = new Color(107, 114, 128);  // #6B7280
    private static final Color LINE_COLOR   = new Color(232, 228, 219);  // #E8E4DB
    private static final Color TABLE_HEADER = new Color(248, 246, 241);  // #F8F6F1

    private static final DateTimeFormatter DISPLAY_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    public static class InvoiceData {
        public String invoiceNumber;
        public String collegeName;
        public String collegeCode;
        public String collegeCity;
        public String collegeState;
        public String collegeEmail;
        public BigDecimal amount;
        public BigDecimal contractAmount;
        public String billingPeriodStart;  // ISO "YYYY-MM-DD"
        public String billingPeriodEnd;    // ISO "YYYY-MM-DD"
        public String description;
        public String generatedDate;       // ISO "YYYY-MM-DD"
        public String dueDate;             // ISO "YYYY-MM-DD" (30 days after generated)
    }

    /**
     * Generates a PDF invoice and returns the raw bytes.
     * Upload to S3 immediately after calling this.
     */
    public static byte[] generatePdf(InvoiceData data) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4, 55, 55, 60, 60);
            PdfWriter.getInstance(document, baos);
            document.open();

            addContent(document, data);

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate invoice PDF: " + e.getMessage(), e);
        }
    }

    private static void addContent(Document doc, InvoiceData d) throws DocumentException {
        Font headerFont    = new Font(Font.HELVETICA, 22, Font.BOLD, BRAND_BLUE);
        Font labelFont     = new Font(Font.HELVETICA, 9, Font.NORMAL, TEXT_MUTED);
        Font valueFont     = new Font(Font.HELVETICA, 10, Font.BOLD, TEXT_DARK);
        Font normalFont    = new Font(Font.HELVETICA, 10, Font.NORMAL, TEXT_DARK);
        Font smallMuted    = new Font(Font.HELVETICA, 8, Font.NORMAL, TEXT_MUTED);
        Font tableHead     = new Font(Font.HELVETICA, 9, Font.BOLD, TEXT_MUTED);
        Font totalFont     = new Font(Font.HELVETICA, 12, Font.BOLD, BRAND_BLUE);
        Font footerFont    = new Font(Font.HELVETICA, 8, Font.NORMAL, TEXT_MUTED);

        // ── Header Row: Logo/Name + INVOICE badge ─────────────────
        PdfPTable header = new PdfPTable(2);
        header.setWidthPercentage(100);
        header.setWidths(new float[]{60, 40});
        header.setSpacingAfter(20);

        // Left: Brand name
        PdfPCell brandCell = new PdfPCell();
        brandCell.setBorder(Rectangle.NO_BORDER);
        brandCell.addElement(new Phrase("Applyra", new Font(Font.HELVETICA, 26, Font.BOLD, BRAND_BLUE)));
        Phrase tagline = new Phrase("Placement Intelligence Platform", new Font(Font.HELVETICA, 9, Font.NORMAL, TEXT_MUTED));
        brandCell.addElement(tagline);
        header.addCell(brandCell);

        // Right: INVOICE title
        PdfPCell invCell = new PdfPCell();
        invCell.setBorder(Rectangle.NO_BORDER);
        invCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph invTitle = new Paragraph("INVOICE", new Font(Font.HELVETICA, 28, Font.BOLD, TEXT_DARK));
        invTitle.setAlignment(Element.ALIGN_RIGHT);
        invCell.addElement(invTitle);
        Paragraph invNum = new Paragraph(d.invoiceNumber, new Font(Font.HELVETICA, 11, Font.NORMAL, TEXT_MUTED));
        invNum.setAlignment(Element.ALIGN_RIGHT);
        invCell.addElement(invNum);
        header.addCell(invCell);

        doc.add(header);

        // ── Divider ───────────────────────────────────────────────
        LineSeparator line = new LineSeparator(1.5f, 100, BRAND_GOLD, Element.ALIGN_CENTER, -2);
        doc.add(line);
        doc.add(Chunk.NEWLINE);

        // ── Billed To / Invoice Details ───────────────────────────
        PdfPTable meta = new PdfPTable(2);
        meta.setWidthPercentage(100);
        meta.setWidths(new float[]{55, 45});
        meta.setSpacingAfter(24);

        // Left: Bill To
        PdfPCell billToCell = new PdfPCell();
        billToCell.setBorder(Rectangle.NO_BORDER);
        billToCell.addElement(new Phrase("BILLED TO", new Font(Font.HELVETICA, 8, Font.BOLD, BRAND_BLUE)));
        billToCell.addElement(Chunk.NEWLINE);
        billToCell.addElement(new Phrase(d.collegeName, valueFont));
        if (d.collegeCode != null && !d.collegeCode.isEmpty()) {
            billToCell.addElement(new Phrase("Code: " + d.collegeCode, labelFont));
        }
        if (d.collegeCity != null || d.collegeState != null) {
            String loc = Stream.of(d.collegeCity, d.collegeState)
                    .filter(s -> s != null && !s.isEmpty())
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");
            if (!loc.isEmpty()) billToCell.addElement(new Phrase(loc, labelFont));
        }
        if (d.collegeEmail != null && !d.collegeEmail.isEmpty()) {
            billToCell.addElement(new Phrase(d.collegeEmail, labelFont));
        }
        meta.addCell(billToCell);

        // Right: Invoice details
        PdfPCell detailsCell = new PdfPCell();
        detailsCell.setBorder(Rectangle.NO_BORDER);
        detailsCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

        PdfPTable detailsTable = new PdfPTable(2);
        detailsTable.setWidthPercentage(100);
        detailsTable.setWidths(new float[]{50, 50});

        addDetailRow(detailsTable, "Issue Date",    fmt(d.generatedDate), labelFont, smallMuted);
        addDetailRow(detailsTable, "Due Date",      fmt(d.dueDate),       labelFont, smallMuted);
        if (d.billingPeriodStart != null || d.billingPeriodEnd != null) {
            String period = Stream.of(fmt(d.billingPeriodStart), fmt(d.billingPeriodEnd))
                    .filter(s -> !s.isEmpty())
                    .reduce((a, b) -> a + " – " + b)
                    .orElse("-");
            addDetailRow(detailsTable, "Period", period, labelFont, smallMuted);
        }

        detailsCell.addElement(detailsTable);
        meta.addCell(detailsCell);

        doc.add(meta);

        // ── Line Items Table ───────────────────────────────────────
        PdfPTable items = new PdfPTable(4);
        items.setWidthPercentage(100);
        items.setWidths(new float[]{45, 20, 15, 20});
        items.setSpacingAfter(20);

        // Table header
        String[] heads = {"Description", "Billing Period", "Qty", "Amount"};
        for (String h : heads) {
            PdfPCell hc = new PdfPCell(new Phrase(h, tableHead));
            hc.setBackgroundColor(TABLE_HEADER);
            hc.setBorderColor(LINE_COLOR);
            hc.setBorderWidth(0.5f);
            hc.setPadding(8);
            hc.setHorizontalAlignment(h.equals("Amount") || h.equals("Qty") ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);
            items.addCell(hc);
        }

        // Single line item: Applyra Platform Service Fee
        String periodStr = "";
        if (d.billingPeriodStart != null && d.billingPeriodEnd != null) {
            periodStr = fmt(d.billingPeriodStart) + " – " + fmt(d.billingPeriodEnd);
        }
        String desc = (d.description != null && !d.description.isEmpty())
                ? d.description
                : "Applyra Platform Service Fee";

        addItemRow(items, desc, periodStr, "1", formatInr(d.amount), normalFont, LINE_COLOR);

        doc.add(items);

        // ── Summary Box ────────────────────────────────────────────
        PdfPTable summary = new PdfPTable(2);
        summary.setWidthPercentage(55);
        summary.setHorizontalAlignment(Element.ALIGN_RIGHT);
        summary.setWidths(new float[]{55, 45});
        summary.setSpacingAfter(30);

        addSummaryRow(summary, "Sub-total",  formatInr(d.amount), labelFont, normalFont, LINE_COLOR, false);
        addSummaryRow(summary, "GST (18%)",  formatInr(d.amount.multiply(BigDecimal.valueOf(0.18))), labelFont, normalFont, LINE_COLOR, false);
        addSummaryRow(summary, "Total Due",  formatInr(d.amount.multiply(BigDecimal.valueOf(1.18))), totalFont, totalFont, BRAND_BLUE, true);

        doc.add(summary);

        // ── Payment Info ───────────────────────────────────────────
        Paragraph payTitle = new Paragraph("Payment Information", new Font(Font.HELVETICA, 9, Font.BOLD, TEXT_DARK));
        payTitle.setSpacingBefore(4);
        doc.add(payTitle);
        doc.add(new Paragraph("Please transfer to the Applyra bank account and reference invoice number " + d.invoiceNumber + " in the transaction.", smallMuted));

        doc.add(Chunk.NEWLINE);
        doc.add(new LineSeparator(0.5f, 100, LINE_COLOR, Element.ALIGN_CENTER, -2));
        doc.add(Chunk.NEWLINE);

        // Footer
        Paragraph footer = new Paragraph(
            "Applyra Placement Intelligence Platform  |  applyra.in  |  support@applyra.in\n"
            + "This is a computer-generated invoice and does not require a physical signature.",
            footerFont
        );
        footer.setAlignment(Element.ALIGN_CENTER);
        doc.add(footer);
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private static void addDetailRow(PdfPTable t, String label, String value, Font lf, Font vf) {
        PdfPCell lc = new PdfPCell(new Phrase(label + ":", lf));
        lc.setBorder(Rectangle.NO_BORDER);
        lc.setPaddingBottom(4);
        t.addCell(lc);
        PdfPCell vc = new PdfPCell(new Phrase(value, vf));
        vc.setBorder(Rectangle.NO_BORDER);
        vc.setHorizontalAlignment(Element.ALIGN_RIGHT);
        vc.setPaddingBottom(4);
        t.addCell(vc);
    }

    private static void addItemRow(PdfPTable t, String desc, String period, String qty, String amount, Font f, Color border) {
        String[] vals = {desc, period, qty, amount};
        for (int i = 0; i < vals.length; i++) {
            PdfPCell c = new PdfPCell(new Phrase(vals[i], f));
            c.setBorderColor(border);
            c.setBorderWidth(0.5f);
            c.setPadding(8);
            c.setHorizontalAlignment(i >= 2 ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);
            t.addCell(c);
        }
    }

    private static void addSummaryRow(PdfPTable t, String label, String value, Font lf, Font vf, Color borderColor, boolean highlight) {
        PdfPCell lc = new PdfPCell(new Phrase(label, lf));
        lc.setBorderColor(borderColor);
        lc.setBorderWidth(highlight ? 1f : 0.5f);
        lc.setPadding(7);
        if (highlight) lc.setBackgroundColor(new Color(240, 248, 255));
        t.addCell(lc);

        PdfPCell vc = new PdfPCell(new Phrase(value, vf));
        vc.setBorderColor(borderColor);
        vc.setBorderWidth(highlight ? 1f : 0.5f);
        vc.setPadding(7);
        vc.setHorizontalAlignment(Element.ALIGN_RIGHT);
        if (highlight) vc.setBackgroundColor(new Color(240, 248, 255));
        t.addCell(vc);
    }

    private static String fmt(String iso) {
        if (iso == null || iso.isEmpty()) return "";
        try {
            return LocalDate.parse(iso).format(DISPLAY_FMT);
        } catch (Exception e) {
            return iso;
        }
    }

    private static String formatInr(BigDecimal amount) {
        if (amount == null) return "₹0.00";
        NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
        return nf.format(amount);
    }

    // Java 11+ stream helper
    private static class Stream {
        static java.util.stream.Stream<String> of(String... parts) {
            return java.util.Arrays.stream(parts);
        }
    }
}

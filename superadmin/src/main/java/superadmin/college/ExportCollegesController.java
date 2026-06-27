package superadmin.college;

import helpers.annotations.SuperAdminRole;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.superadmin.SuperAdminAccessMiddleware;
import models.repos.*;
import models.services.EmailService;
import models.sql.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * POST /admin/colleges/export
 * Generates a CSV report of all colleges with placement data and emails it to the requesting admin.
 */
@SuperAdminRole
public enum ExportCollegesController implements BaseController {

    INSTANCE;

    @Override
    public void handle(RoutingContext event) {
        SuperAdminAccessMiddleware.INSTANCE.with(event, new ArrayList<>(), this.getClass())
                .map(req -> {
                    String adminEmail = req.getUser().getEmail();
                    String adminName = req.getUser().getName();

                    List<College> colleges = CollegeRepository.INSTANCE.findAll();

                    // Build CSV
                    StringBuilder csv = new StringBuilder();
                    csv.append("College Name,Code,City,State,University,Status,Total Students,Placed Students,Unplaced Students,Placement Rate (%),Total Drives,Active Drives,Total Companies,Total Offers,Highest CTC,Average CTC,Lowest CTC\n");

                    for (College c : colleges) {
                        List<Student> students = StudentRepository.INSTANCE.byCollege(c.getId());
                        List<Student> placed = StudentRepository.INSTANCE.findPlaced(c.getId());
                        int totalStudents = students.size();
                        int placedStudents = placed.size();
                        int unplacedStudents = totalStudents - placedStudents;
                        double placementRate = totalStudents > 0
                                ? (double) placedStudents / totalStudents * 100
                                : 0;

                        // Drives for this college
                        List<Drive> drives = DriveRepository.INSTANCE.byCollege(c.getId());
                        int totalDrives = drives.size();
                        long activeDrives = drives.stream()
                                .filter(d -> d.getStatus() != null
                                        && !d.getStatus().name().equals("COMPLETED")
                                        && !d.getStatus().name().equals("CANCELLED"))
                                .count();

                        // Companies linked to this college
                        List<CompanyCollege> companyLinks = CompanyCollegeRepository.INSTANCE.byCollege(c.getId());
                        int totalCompanies = companyLinks.size();

                        // Offers from drives of this college
                        BigDecimal highestCtc = BigDecimal.ZERO;
                        BigDecimal lowestCtc = null;
                        BigDecimal totalCtc = BigDecimal.ZERO;
                        int ctcCount = 0;
                        int totalOffers = 0;

                        for (Drive d : drives) {
                            List<Offer> offers = OfferRepository.INSTANCE.byDrive(d.getId());
                            totalOffers += offers.size();
                            if (d.getCtcOffered() != null) {
                                for (Offer o : offers) {
                                    BigDecimal ctc = d.getCtcOffered();
                                    totalCtc = totalCtc.add(ctc);
                                    ctcCount++;
                                    if (ctc.compareTo(highestCtc) > 0) highestCtc = ctc;
                                    if (lowestCtc == null || ctc.compareTo(lowestCtc) < 0) lowestCtc = ctc;
                                }
                            }
                        }

                        BigDecimal avgCtc = ctcCount > 0
                                ? totalCtc.divide(BigDecimal.valueOf(ctcCount), 2, RoundingMode.HALF_UP)
                                : BigDecimal.ZERO;

                        String status = !c.isVerified() ? "Pending" : (c.isActive() ? "Active" : "Inactive");

                        csv.append(escapeCsv(c.getName())).append(",");
                        csv.append(escapeCsv(c.getCode())).append(",");
                        String cityName = "";
                        String stateName = "";
                        if (c.getCityId() != null) {
                            City ct = io.ebean.DB.find(City.class, c.getCityId());
                            if (ct != null) cityName = ct.getName();
                        }
                        if (c.getStateId() != null) {
                            States st = io.ebean.DB.find(States.class, c.getStateId());
                            if (st != null) stateName = st.getName();
                        }
                        csv.append(escapeCsv(cityName)).append(",");
                        csv.append(escapeCsv(stateName)).append(",");
                        csv.append(escapeCsv(c.getUniversity() != null ? c.getUniversity() : "")).append(",");
                        csv.append(status).append(",");
                        csv.append(totalStudents).append(",");
                        csv.append(placedStudents).append(",");
                        csv.append(unplacedStudents).append(",");
                        csv.append(String.format("%.1f", placementRate)).append(",");
                        csv.append(totalDrives).append(",");
                        csv.append(activeDrives).append(",");
                        csv.append(totalCompanies).append(",");
                        csv.append(totalOffers).append(",");
                        csv.append(highestCtc).append(",");
                        csv.append(avgCtc).append(",");
                        csv.append(lowestCtc != null ? lowestCtc : BigDecimal.ZERO).append("\n");
                    }

                    // Send email with CSV as inline content (Brevo doesn't support attachments easily via API)
                    // We'll send an HTML email with the CSV data as a downloadable data URI
                    String csvContent = csv.toString();
                    String htmlBody = buildExportEmailHtml(adminName, colleges.size(), csvContent);

                    new Thread(() -> {
                        try {
                            EmailService.sendEmail(adminEmail,
                                    "Applyra — College Placement Report Export",
                                    htmlBody
                            ).subscribe(
                                    sent -> System.out.println("[ExportColleges] Report email " + (sent ? "sent" : "failed") + " to " + adminEmail),
                                    err -> System.err.println("[ExportColleges] Email error: " + err.getMessage())
                            );
                        } catch (Exception e) {
                            System.err.println("[ExportColleges] Email thread error: " + e.getMessage());
                        }
                    }, "export-colleges-email").start();

                    Map<String, Object> response = new LinkedHashMap<>();
                    response.put("message", "Report is being generated and will be sent to " + adminEmail);
                    response.put("collegeCount", colleges.size());
                    response.put("email", adminEmail);
                    // Also return the CSV so the frontend can download it directly
                    response.put("csv", csvContent);
                    return response;
                })
                .subscribe(
                        o -> ResponseUtils.INSTANCE.writeJsonResponse(event, o),
                        error -> ResponseUtils.INSTANCE.handleError(event, error)
                );
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private String buildExportEmailHtml(String adminName, int collegeCount, String csvContent) {
        // Encode CSV as base64 for data URI download link
        String base64Csv = Base64.getEncoder().encodeToString(csvContent.getBytes());

        return "<!DOCTYPE html><html><body style=\"font-family: 'Inter', Arial, sans-serif; background: #F5F3EE; padding: 40px 0;\">"
                + "<div style=\"max-width: 600px; margin: 0 auto; background: #fff; border-radius: 16px; overflow: hidden; box-shadow: 0 2px 8px rgba(0,0,0,0.06);\">"
                + "<div style=\"background: linear-gradient(135deg, #C4A55A 0%, #D4B46A 50%, #E8CC7A 100%); padding: 32px; text-align: center;\">"
                + "<h1 style=\"color: #1A1A1A; margin: 0; font-size: 24px;\">Applyra</h1>"
                + "<p style=\"color: #5C4E35; margin: 8px 0 0; font-size: 14px;\">College Placement Report</p>"
                + "</div>"
                + "<div style=\"padding: 32px;\">"
                + "<p style=\"color: #1A1A1A; font-size: 16px;\">Hi " + (adminName != null ? adminName : "Admin") + ",</p>"
                + "<p style=\"color: #6B665C; font-size: 14px; line-height: 1.6;\">Your college placement report has been generated successfully. "
                + "The report includes data for <strong>" + collegeCount + " colleges</strong> with student counts, placement rates, CTC details, and more.</p>"
                + "<p style=\"color: #6B665C; font-size: 14px; line-height: 1.6;\">The CSV data is included below. Copy and paste it into a text file and save as .csv to open in Excel.</p>"
                + "<div style=\"background: #F5F3EE; border-radius: 8px; padding: 16px; margin: 20px 0; overflow-x: auto;\">"
                + "<pre style=\"font-size: 11px; color: #5C4E35; white-space: pre-wrap; word-break: break-all; margin: 0;\">" + csvContent.replace("<", "&lt;").replace(">", "&gt;") + "</pre>"
                + "</div>"
                + "<p style=\"color: #A09A8E; font-size: 12px;\">Generated on " + new java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a").format(new java.util.Date()) + "</p>"
                + "</div>"
                + "</div></body></html>";
    }
}

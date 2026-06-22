package superadmin;

import helpers.annotations.SuperAdminRole;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.superadmin.SuperAdminAccessMiddleware;
import models.body.SuperAdminLoginRequest;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * GET /admin/gstin/:gstin
 *
 * Validates and looks up a GSTIN via the public GST portal API.
 * Returns pre-filled college details: legal name, trade name, state, city, pincode, status.
 * Used in onboarding Step 1 to auto-populate college information.
 */
@SuperAdminRole
public enum GstinLookupController implements BaseController {

    INSTANCE;

    // Maps GST state code (first 2 digits of GSTIN) → state name
    private static final Map<String, String> STATE_CODE_MAP = new HashMap<>();
    static {
        STATE_CODE_MAP.put("01", "Jammu and Kashmir");
        STATE_CODE_MAP.put("02", "Himachal Pradesh");
        STATE_CODE_MAP.put("03", "Punjab");
        STATE_CODE_MAP.put("04", "Chandigarh");
        STATE_CODE_MAP.put("05", "Uttarakhand");
        STATE_CODE_MAP.put("06", "Haryana");
        STATE_CODE_MAP.put("07", "Delhi");
        STATE_CODE_MAP.put("08", "Rajasthan");
        STATE_CODE_MAP.put("09", "Uttar Pradesh");
        STATE_CODE_MAP.put("10", "Bihar");
        STATE_CODE_MAP.put("11", "Sikkim");
        STATE_CODE_MAP.put("12", "Arunachal Pradesh");
        STATE_CODE_MAP.put("13", "Nagaland");
        STATE_CODE_MAP.put("14", "Manipur");
        STATE_CODE_MAP.put("15", "Mizoram");
        STATE_CODE_MAP.put("16", "Tripura");
        STATE_CODE_MAP.put("17", "Meghalaya");
        STATE_CODE_MAP.put("18", "Assam");
        STATE_CODE_MAP.put("19", "West Bengal");
        STATE_CODE_MAP.put("20", "Jharkhand");
        STATE_CODE_MAP.put("21", "Odisha");
        STATE_CODE_MAP.put("22", "Chhattisgarh");
        STATE_CODE_MAP.put("23", "Madhya Pradesh");
        STATE_CODE_MAP.put("24", "Gujarat");
        STATE_CODE_MAP.put("25", "Daman and Diu");
        STATE_CODE_MAP.put("26", "Dadra and Nagar Haveli");
        STATE_CODE_MAP.put("27", "Maharashtra");
        STATE_CODE_MAP.put("28", "Andhra Pradesh");
        STATE_CODE_MAP.put("29", "Karnataka");
        STATE_CODE_MAP.put("30", "Goa");
        STATE_CODE_MAP.put("31", "Lakshadweep");
        STATE_CODE_MAP.put("32", "Kerala");
        STATE_CODE_MAP.put("33", "Tamil Nadu");
        STATE_CODE_MAP.put("34", "Puducherry");
        STATE_CODE_MAP.put("35", "Andaman and Nicobar Islands");
        STATE_CODE_MAP.put("36", "Telangana");
        STATE_CODE_MAP.put("37", "Andhra Pradesh");
        STATE_CODE_MAP.put("38", "Ladakh");
        STATE_CODE_MAP.put("97", "Other Territory");
        STATE_CODE_MAP.put("99", "Centre Jurisdiction");
    }

    @Override
    public void handle(RoutingContext event) {
        SuperAdminAccessMiddleware.INSTANCE.with(event, new ArrayList<>(), this.getClass())
                .map(req -> lookup(req, event))
                .subscribe(
                        o -> ResponseUtils.INSTANCE.writeJsonResponse(event, o),
                        error -> ResponseUtils.INSTANCE.handleError(event, error)
                );
    }

    private Object lookup(SuperAdminLoginRequest req, RoutingContext rc) {
        String gstin = rc.pathParam("gstin");
        if (gstin == null || gstin.isBlank())
            throw new helpers.customErrors.RoutingError("GSTIN is required");

        gstin = gstin.toUpperCase().trim();

        // Validate format: 2-digit state code + 10-char PAN + 1 entity + Z + 1 check
        if (!gstin.matches("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$"))
            throw new helpers.customErrors.RoutingError("Invalid GSTIN format. Expected: 27AAPFU0939F1ZV");

        String stateCode = gstin.substring(0, 2);
        String stateName = STATE_CODE_MAP.getOrDefault(stateCode, null);

        // Try to fetch full details from the public GST portal
        Map<String, Object> result = new HashMap<>();
        result.put("gstin", gstin);
        result.put("stateCode", stateCode);
        if (stateName != null) result.put("stateName", stateName);

        try {
            Map<String, Object> gstData = fetchFromGstPortal(gstin);
            result.putAll(gstData);
            result.put("source", "gst_portal");
        } catch (Exception e) {
            System.err.println("[GSTIN] Portal lookup failed: " + e.getMessage() + " — returning state from prefix only");
            result.put("source", "prefix_only");
            result.put("message", "Could not fetch full details. State derived from GSTIN prefix.");
        }

        return result;
    }

    /**
     * Calls the GST Portal taxpayer search API (same endpoint the portal UI uses).
     * Tries two known endpoints in sequence; throws if both fail.
     */
    private Map<String, Object> fetchFromGstPortal(String gstin) throws Exception {
        // Primary: official GST services API
        String[] endpoints = {
            "https://services.gst.gov.in/services/api/search/taxpayer?gstin=" + gstin,
            "https://api.gst.gov.in/commonapi/v1.1/search?action=TP&gstin=" + gstin
        };

        Exception lastEx = null;
        for (String apiUrl : endpoints) {
            try {
                String json = httpGet(apiUrl);
                if (json != null && !json.isBlank() && json.contains("lgnm")) {
                    return parseGstResponse(json);
                }
            } catch (Exception e) {
                lastEx = e;
                System.err.println("[GSTIN] Endpoint failed (" + apiUrl + "): " + e.getMessage());
            }
        }
        throw new Exception("All GST endpoints failed: " + (lastEx != null ? lastEx.getMessage() : "no data"));
    }

    private String httpGet(String apiUrl) throws Exception {
        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setInstanceFollowRedirects(true);
        conn.setRequestProperty("Accept", "application/json, text/plain, */*");
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        conn.setRequestProperty("Referer", "https://www.gst.gov.in/");
        conn.setRequestProperty("Origin", "https://www.gst.gov.in");
        conn.setConnectTimeout(6000);
        conn.setReadTimeout(6000);

        int status = conn.getResponseCode();
        if (status != 200) throw new Exception("HTTP " + status);

        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) sb.append(line);
        br.close();
        conn.disconnect();
        return sb.toString();
    }

    private Map<String, Object> parseGstResponse(String json) {
        Map<String, Object> out = new HashMap<>();

        // The services.gst.gov.in response wraps data under "taxpayerInfo"
        String body = json;
        int tiStart = json.indexOf("\"taxpayerInfo\"");
        if (tiStart >= 0) body = json.substring(tiStart);

        String legalName = jsonField(body, "lgnm");
        String tradeName = jsonField(body, "tradeNam");
        String status    = jsonField(body, "sts");
        String bizType   = jsonField(body, "ctb");   // constitution of business

        if (legalName != null) out.put("legalName",  titleCase(legalName));
        if (tradeName != null) out.put("tradeName",  titleCase(tradeName));
        if (status    != null) out.put("gstStatus",  status);
        if (bizType   != null) out.put("businessType", bizType);

        // Suggested college name: trade name preferred, fallback to legal name
        String suggestedName = (tradeName != null && !tradeName.isBlank()) ? tradeName : legalName;
        if (suggestedName != null) out.put("suggestedName", titleCase(suggestedName));

        // Parse primary address block
        try {
            int pradrStart = body.indexOf("\"pradr\"");
            if (pradrStart >= 0) {
                String addrBlock = body.substring(pradrStart);

                // Flat address string (services.gst.gov.in provides "adr")
                String flatAdr = jsonField(addrBlock, "adr");
                if (flatAdr != null && !flatAdr.isBlank()) out.put("address", titleCase(flatAdr));

                // Inner addr object fields
                String pincode = jsonField(addrBlock, "pncd");
                String city    = firstNonBlank(jsonField(addrBlock, "city"), jsonField(addrBlock, "dst"));
                String loc     = jsonField(addrBlock, "loc");
                String bno     = jsonField(addrBlock, "bno");
                String bnm     = jsonField(addrBlock, "bnm");
                String st      = jsonField(addrBlock, "st");

                if (pincode != null) out.put("pincode", pincode);
                if (city    != null) out.put("city",    titleCase(city));
                if (loc     != null) out.put("locality", titleCase(loc));

                // Build address from parts if flat address not available
                if (flatAdr == null) {
                    StringBuilder addr = new StringBuilder();
                    if (bno != null && !bno.isBlank()) addr.append(bno).append(", ");
                    if (bnm != null && !bnm.isBlank()) addr.append(bnm).append(", ");
                    if (st  != null && !st.isBlank())  addr.append(st).append(", ");
                    if (loc != null && !loc.isBlank()) addr.append(loc);
                    String built = addr.toString().replaceAll(",\\s*$", "").trim();
                    if (!built.isBlank()) out.put("address", titleCase(built));
                }
            }
        } catch (Exception e) {
            System.err.println("[GSTIN] Address parse error: " + e.getMessage());
        }

        return out;
    }

    private String firstNonBlank(String... values) {
        for (String v : values) if (v != null && !v.isBlank()) return v;
        return null;
    }

    /** Extracts a flat string field from JSON: "key":"value" */
    private String jsonField(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start < 0) return null;
        start += search.length();
        int end = json.indexOf("\"", start);
        if (end < 0) return null;
        String val = json.substring(start, end).trim();
        return val.isEmpty() ? null : val;
    }

    private String titleCase(String s) {
        if (s == null || s.isBlank()) return s;
        String[] words = s.toLowerCase().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!w.isEmpty()) {
                sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(" ");
            }
        }
        return sb.toString().trim();
    }
}

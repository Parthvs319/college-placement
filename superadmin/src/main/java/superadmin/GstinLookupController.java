package superadmin;

import helpers.annotations.SuperAdminRole;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.superadmin.SuperAdminAccessMiddleware;
import models.body.SuperAdminLoginRequest;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * GET /admin/gstin/:gstin
 *
 * Validates GSTIN format and fetches business details.
 * Equivalent of the Retrofit call:
 *   @POST("/api/validate/gstin.v2")
 *   Call<GstDetails> validateGstin(@Header("Authorization") String auth, @Body GstinRequest request)
 *
 * Required env vars:
 *   GSTIN_API_BASE_URL  — base URL of your KYC provider  e.g. https://api.digitap.ai
 *   GSTIN_API_KEY       — bearer token                    e.g. eyJhbGci...
 *   GSTIN_API_PATH      — POST path (optional)            default: /api/validate/gstin.v2
 *
 * Cashfree sandbox (free, no credit card):
 *   GSTIN_API_BASE_URL = https://sandbox.cashfree.com
 *   GSTIN_API_PATH     = /verification/gstin
 *   GSTIN_API_KEY      = <client_id>:<client_secret>   (joined with colon, code splits them)
 *
 * Falls back to state-from-GSTIN-prefix if no env vars are set.
 */
@SuperAdminRole
public enum GstinLookupController implements BaseController {

    INSTANCE;

    private static final String GSTIN_API_BASE     = System.getenv().getOrDefault("GSTIN_API_BASE_URL", "https://kyc-api.surepass.app");
    private static final String GSTIN_API_KEY      = System.getenv("GSTIN_API_KEY");
    private static final String GSTIN_API_PATH     = System.getenv().getOrDefault("GSTIN_API_PATH",     "/api/v1/corporate/gstin");
    // Surepass uses "id" as body key; other providers may use "gstin"
    private static final String GSTIN_BODY_KEY     = System.getenv().getOrDefault("GSTIN_BODY_KEY",     "id");

    // GST state code prefix → state name
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
        if (gstin == null || gstin.isBlank()) throw new RoutingError("GSTIN is required");

        gstin = gstin.toUpperCase().trim();
        if (!gstin.matches("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$"))
            throw new RoutingError("Invalid GSTIN format. Example: 27AAPFU0939F1ZV");

        String stateCode = gstin.substring(0, 2);
        String stateName = STATE_CODE_MAP.getOrDefault(stateCode, null);

        Map<String, Object> result = new HashMap<>();
        result.put("gstin",     gstin);
        result.put("stateCode", stateCode);
        if (stateName != null) result.put("stateName", stateName);

        // No API key configured — return prefix-only result
        if (GSTIN_API_BASE == null || GSTIN_API_BASE.isBlank() ||
            GSTIN_API_KEY  == null || GSTIN_API_KEY.isBlank()) {
            result.put("source",  "prefix_only");
            result.put("message", "Set GSTIN_API_BASE_URL and GSTIN_API_KEY to enable full lookup.");
            return result;
        }

        try {
            Map<String, Object> data = callGstinApi(gstin);
            result.putAll(data);
            result.put("source", "api");
        } catch (Exception e) {
            System.err.println("[GSTIN] API lookup failed: " + e.getMessage());
            result.put("source",  "prefix_only");
            result.put("message", "Lookup failed — state derived from GSTIN prefix.");
        }

        return result;
    }

    /**
     * Generic GSTIN POST call — matches the Retrofit pattern:
     *   @POST(GSTIN_API_PATH)
     *   Call<GstDetails> validateGstin(@Header("Authorization") String auth, @Body GstinRequest)
     *
     * Request body:  { "gstin": "27AAPFU0939F1ZV" }
     * Authorization: Bearer <GSTIN_API_KEY>
     *
     * Special case — Cashfree uses two separate headers instead of Bearer:
     *   If GSTIN_API_KEY contains ":" it is split into x-client-id : x-client-secret
     */
    private Map<String, Object> callGstinApi(String gstin) throws Exception {
        String endpoint = GSTIN_API_BASE.replaceAll("/+$", "") + GSTIN_API_PATH;
        URL url = new URL(endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept",       "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);

        // Auth header — Bearer by default; split for Cashfree (clientId:clientSecret)
        if (GSTIN_API_KEY.contains(":")) {
            String[] parts = GSTIN_API_KEY.split(":", 2);
            conn.setRequestProperty("x-client-id",     parts[0]);
            conn.setRequestProperty("x-client-secret", parts[1]);
        } else {
            conn.setRequestProperty("Authorization", "Bearer " + GSTIN_API_KEY);
        }

        // Request body key varies by provider: Surepass → "id", others → "gstin"
        String body = "{\"" + GSTIN_BODY_KEY + "\":\"" + gstin + "\"}";
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }

        int status = conn.getResponseCode();
        if (status != 200) {
            String errBody = "";
            try {
                BufferedReader eb = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                StringBuilder s = new StringBuilder(); String l;
                while ((l = eb.readLine()) != null) s.append(l);
                errBody = s.toString();
            } catch (Exception ignored) {}
            throw new Exception("HTTP " + status + " — " + errBody);
        }

        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder sb = new StringBuilder(); String line;
        while ((line = br.readLine()) != null) sb.append(line);
        br.close();
        conn.disconnect();

        return parseResponse(sb.toString());
    }

    /**
     * Parses the API response — handles the most common field names used by
     * Indian KYC providers (Digitap, Cashfree, Surepass, etc.).
     */
    private Map<String, Object> parseResponse(String json) {
        Map<String, Object> out = new HashMap<>();

        // Unwrap common envelope keys: data / result / response
        String body = json;
        for (String key : new String[]{"\"data\"", "\"result\"", "\"response\""}) {
            int idx = json.indexOf(key);
            if (idx >= 0) { body = json.substring(idx); break; }
        }

        // Business / trade name
        String name = firstOf(body,
            "business_name", "trade_name", "tradeNam", "tradeName",
            "company_name",  "name");

        // Legal name
        String legalName = firstOf(body, "legal_name", "lgnm", "legalName");

        // Status, state, city, address, pincode
        String gstStatus = firstOf(body, "gstin_status", "sts", "status", "gstStatus");
        String state     = firstOf(body, "state", "stateName", "stcd");
        String city      = firstOf(body, "city",  "dst", "district");
        String address   = firstOf(body, "address", "adr", "pradr");
        String pincode   = firstOf(body, "pincode", "pncd", "pin_code");
        String bizType   = firstOf(body, "constitution_of_business", "ctb", "business_type");

        if (name      != null) { out.put("tradeName",    titleCase(name));      out.put("suggestedName", titleCase(name)); }
        if (legalName != null)   out.put("legalName",    titleCase(legalName));
        if (gstStatus != null)   out.put("gstStatus",    gstStatus);
        if (state     != null)   out.put("stateName",    titleCase(state));
        if (city      != null)   out.put("city",         titleCase(city));
        if (address   != null)   out.put("address",      titleCase(address));
        if (pincode   != null)   out.put("pincode",      pincode);
        if (bizType   != null)   out.put("businessType", bizType);

        // If no trade name found, fall back to legal name as suggestion
        if (name == null && legalName != null) out.put("suggestedName", titleCase(legalName));

        return out;
    }

    /** Tries each field name in order, returns first non-blank string value found */
    private String firstOf(String json, String... keys) {
        for (String key : keys) {
            String val = jsonStringField(json, key);
            if (val != null) return val;
        }
        return null;
    }

    /** Extracts "key":"value" from flat JSON */
    private String jsonStringField(String json, String key) {
        String search = "\"" + key + "\":\"";
        int start = json.indexOf(search);
        if (start < 0) return null;
        start += search.length();
        int end = json.indexOf("\"", start);
        if (end < 0) return null;
        String val = json.substring(start, end).trim();
        return (val.isEmpty() || val.equalsIgnoreCase("null")) ? null : val;
    }

    private String titleCase(String s) {
        if (s == null || s.isBlank()) return s;
        StringBuilder sb = new StringBuilder();
        for (String w : s.toLowerCase().split("\\s+"))
            if (!w.isEmpty()) sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(" ");
        return sb.toString().trim();
    }
}

package models.services;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.textract.TextractClient;
import software.amazon.awssdk.services.textract.model.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.regex.*;

/**
 * Extracts text from contract PDFs and parses contract fields.
 *
 * Text extraction strategy:
 *   1. Apache PDFBox — fast, works for digital/text-based PDFs
 *   2. AWS Textract fallback — handles scanned/image PDFs
 *
 * Field parsing strategy:
 *   a. Google Gemini 1.5 Flash — if GEMINI_API_KEY is set.
 *      Handles any contract format, including ones where fee is described
 *      in non-standard language or is absent from the main body.
 *   b. Regex fallback — used when API key not configured or Gemini call fails.
 *
 * Extracted fields: contractAmount, validFrom, validTo, tpoEmail, tpoName, collegeName
 */
public class ContractTextExtractor {

    private static final int MIN_TEXT_LENGTH = 100;

    // AWS credentials for Textract
    private static final String ACCESS_KEY = System.getenv().getOrDefault("S3_ACCESS_KEY", "");
    private static final String SECRET_KEY = System.getenv().getOrDefault("S3_SECRET_KEY", "");
    private static final String REGION     = System.getenv().getOrDefault("S3_REGION", "ap-south-1");

    // Google Gemini API config
    private static final String GEMINI_API_KEY = System.getenv("GEMINI_API_KEY");
    private static final String GEMINI_API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=";

    // ── Public Result ─────────────────────────────────────────────────

    public static class ContractExtractResult {
        public String rawText;
        public String contractAmount;   // integer string, e.g. "150000"
        public String validFrom;        // ISO date "YYYY-MM-DD"
        public String validTo;          // ISO date "YYYY-MM-DD"
        public String tpoEmail;
        public String tpoName;
        public String collegeName;      // extracted by AI
        public String extractionMethod; // "pdfbox" | "textract" | "none", optionally suffixed "+claude"
        public Map<String, Double> confidence = new HashMap<>();
    }

    // ── Main Entry Point ──────────────────────────────────────────────

    /**
     * Extract contract fields from raw PDF bytes.
     */
    public static ContractExtractResult extract(byte[] pdfBytes) {
        ContractExtractResult result = new ContractExtractResult();

        // ── Step 1: Extract raw text ──────────────────────────────────
        String text = extractWithPdfBox(pdfBytes);
        if (text != null && text.trim().length() >= MIN_TEXT_LENGTH) {
            result.rawText = text;
            result.extractionMethod = "pdfbox";
        } else {
            System.out.println("[ContractOCR] PDFBox text too short ("
                    + (text != null ? text.trim().length() : 0) + " chars), trying Textract...");
            String textractText = extractWithTextract(pdfBytes);
            if (textractText != null && !textractText.isBlank()) {
                result.rawText = textractText;
                result.extractionMethod = "textract";
            } else {
                result.rawText = text != null ? text : "";
                result.extractionMethod = "none";
            }
        }

        // ── Step 2: Parse fields ──────────────────────────────────────
        if (result.rawText != null && !result.rawText.isBlank()) {
            if (GEMINI_API_KEY != null && !GEMINI_API_KEY.isBlank()) {
                parseWithGemini(result);
            } else {
                parseWithRegex(result);
            }
        }

        return result;
    }

    // ═══════════════════════════════════════════════════════════════════
    // TEXT EXTRACTION
    // ═══════════════════════════════════════════════════════════════════

    private static String extractWithPdfBox(byte[] bytes) {
        try (PDDocument doc = PDDocument.load(bytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(doc);
        } catch (Exception e) {
            System.err.println("[ContractOCR] PDFBox failed: " + e.getMessage());
            return null;
        }
    }

    private static String extractWithTextract(byte[] bytes) {
        if (ACCESS_KEY.isEmpty() || SECRET_KEY.isEmpty()) {
            System.out.println("[ContractOCR] Textract skipped — AWS credentials not configured");
            return null;
        }
        try {
            TextractClient textract = TextractClient.builder()
                    .region(Region.of(REGION))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(ACCESS_KEY, SECRET_KEY)))
                    .build();

            DetectDocumentTextRequest req = DetectDocumentTextRequest.builder()
                    .document(Document.builder()
                            .bytes(SdkBytes.fromByteArray(bytes))
                            .build())
                    .build();

            DetectDocumentTextResponse response = textract.detectDocumentText(req);

            StringBuilder sb = new StringBuilder();
            for (Block block : response.blocks()) {
                if (block.blockType() == BlockType.LINE && block.text() != null) {
                    sb.append(block.text()).append("\n");
                }
            }
            textract.close();
            return sb.toString();
        } catch (Exception e) {
            System.err.println("[ContractOCR] Textract failed: " + e.getMessage());
            return null;
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // GEMINI AI FIELD PARSING
    // ═══════════════════════════════════════════════════════════════════

    private static void parseWithGemini(ContractExtractResult r) {
        // Limit to ~4000 chars to keep token cost low
        String textSnippet = r.rawText.length() > 4000
                ? r.rawText.substring(0, 4000) + "\n[...truncated...]"
                : r.rawText;

        String prompt =
                "Extract the following fields from this contract document and return ONLY valid JSON:\n" +
                "- contractAmount: annual service/subscription/platform fee in INR as integer string (digits only, no commas/symbols). " +
                "Only set if an explicit numeric value is stated. If fee is \"per commercial schedule\" or TBD, return null.\n" +
                "- validFrom: contract start/commencement/effective date in YYYY-MM-DD format (null if not found)\n" +
                "- validTo: contract end/expiry date in YYYY-MM-DD format (null if not found)\n" +
                "- tpoEmail: email of the Training & Placement Officer or college-side contact (null if not found)\n" +
                "- tpoName: full name of TPO or primary contact person from the college (null if not found)\n" +
                "- collegeName: full official name of the educational institution/college (null if not found)\n" +
                "\n" +
                "Rules:\n" +
                "1. Return ONLY the JSON object — no markdown fences, no explanation.\n" +
                "2. For contractAmount: extract only if a clear numeric fee is stated for this agreement.\n" +
                "3. For dates: convert any format (\"21 June 2026\", \"21/06/2026\") to YYYY-MM-DD.\n" +
                "4. For collegeName: extract the institution name, not Applyra.\n" +
                "\n" +
                "Contract text:\n" + textSnippet;

        try {
            // JSON-escape the prompt for inline embedding
            String escapedPrompt = prompt
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");

            // Gemini request format
            String requestBody =
                    "{\"contents\":[{\"parts\":[{\"text\":\"" + escapedPrompt + "\"}]}]," +
                    "\"generationConfig\":{\"maxOutputTokens\":512,\"temperature\":0.1}}";

            HttpClient httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(15))
                    .build();

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(GEMINI_API_URL + GEMINI_API_KEY))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String geminiJson = extractTextFromGeminiResponse(response.body());
                if (geminiJson != null) {
                    applyAIJson(r, geminiJson);
                    r.extractionMethod = r.extractionMethod + "+gemini";
                    System.out.println("[ContractOCR] Gemini extraction successful: " + geminiJson);
                } else {
                    System.err.println("[ContractOCR] Could not parse Gemini response: "
                            + response.body().substring(0, Math.min(300, response.body().length())));
                    parseWithRegex(r);
                }
            } else {
                System.err.println("[ContractOCR] Gemini API error " + response.statusCode()
                        + ": " + response.body().substring(0, Math.min(300, response.body().length())));
                parseWithRegex(r);
            }

        } catch (Exception e) {
            System.err.println("[ContractOCR] Gemini call failed: " + e.getMessage());
            parseWithRegex(r);
        }
    }

    /**
     * Parses the text content from Gemini's API response envelope.
     *
     * Gemini response shape:
     *   { "candidates": [{ "content": { "parts": [{ "text": "{...}" }] } }] }
     *
     * Extracts the value of the first "text" key and returns it if it's a JSON object.
     */
    private static String extractTextFromGeminiResponse(String apiResponse) {
        try {
            // Gemini and Claude both use "text":" as the key for generated content
            int idx = apiResponse.indexOf("\"text\":\"");
            if (idx < 0) return null;
            idx += 8; // skip past "text":"

            // Unescape the JSON string value manually
            StringBuilder sb = new StringBuilder();
            boolean escaped = false;
            for (int i = idx; i < apiResponse.length(); i++) {
                char c = apiResponse.charAt(i);
                if (escaped) {
                    switch (c) {
                        case 'n':  sb.append('\n'); break;
                        case 'r':  sb.append('\r'); break;
                        case 't':  sb.append('\t'); break;
                        case '"':  sb.append('"');  break;
                        case '\\': sb.append('\\'); break;
                        default:   sb.append(c);    break;
                    }
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    break; // end of string value
                } else {
                    sb.append(c);
                }
            }

            String text = sb.toString().trim();

            // Strip markdown code fences if Gemini wrapped the JSON in ```json ... ```
            if (text.startsWith("```")) {
                text = text.replaceFirst("^```[a-zA-Z]*\\n?", "")
                           .replaceFirst("\\n?```$", "")
                           .trim();
            }

            return text.startsWith("{") ? text : null;

        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Apply fields from AI JSON response onto the result object.
     */
    private static void applyAIJson(ContractExtractResult r, String json) {
        r.contractAmount = jsonStringField(json, "contractAmount");
        r.validFrom      = jsonStringField(json, "validFrom");
        r.validTo        = jsonStringField(json, "validTo");
        r.tpoEmail       = jsonStringField(json, "tpoEmail");
        r.tpoName        = jsonStringField(json, "tpoName");
        r.collegeName    = jsonStringField(json, "collegeName");

        // Normalize contractAmount: digits only
        if (r.contractAmount != null) {
            String digits = r.contractAmount.replaceAll("[^0-9]", "");
            r.contractAmount = digits.isEmpty() ? null : digits;
        }

        // AI-extracted fields get higher confidence than regex
        r.confidence.put("contractAmount", r.contractAmount != null ? 0.90 : 0.0);
        r.confidence.put("validFrom",      r.validFrom      != null ? 0.92 : 0.0);
        r.confidence.put("validTo",        r.validTo        != null ? 0.92 : 0.0);
        r.confidence.put("tpoEmail",       r.tpoEmail       != null ? 0.95 : 0.0);
        r.confidence.put("tpoName",        r.tpoName        != null ? 0.85 : 0.0);
        r.confidence.put("collegeName",    r.collegeName    != null ? 0.90 : 0.0);
    }

    /**
     * Extract a nullable string field from a flat JSON object string.
     * Matches: "key": "value"  OR  "key": null
     */
    private static String jsonStringField(String json, String key) {
        Pattern p = Pattern.compile(
                "\"" + key + "\"\\s*:\\s*(?:\"((?:[^\"\\\\]|\\\\.)*)\"|null)",
                Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(json);
        if (!m.find()) return null;
        String val = m.group(1); // null if JSON value was literal null
        if (val == null) return null;
        val = val.trim();
        return val.isEmpty() ? null : val;
    }

    // ═══════════════════════════════════════════════════════════════════
    // REGEX FIELD PARSING (fallback when Claude is unavailable)
    // ═══════════════════════════════════════════════════════════════════

    private static void parseWithRegex(ContractExtractResult r) {
        if (r.rawText == null || r.rawText.isBlank()) return;
        String text = r.rawText;

        r.contractAmount = extractAmount(text);
        r.tpoEmail       = extractEmail(text);
        r.tpoName        = extractTpoName(text);

        String[] dates = extractDates(text);
        r.validFrom = dates[0];
        r.validTo   = dates[1];

        r.confidence.put("contractAmount", r.contractAmount != null ? 0.85 : 0.0);
        r.confidence.put("validFrom",      r.validFrom      != null ? 0.80 : 0.0);
        r.confidence.put("validTo",        r.validTo        != null ? 0.80 : 0.0);
        r.confidence.put("tpoEmail",       r.tpoEmail       != null ? 0.95 : 0.0);
        r.confidence.put("tpoName",        r.tpoName        != null ? 0.70 : 0.0);
    }

    // ── Amount Extraction ─────────────────────────────────────────────

    private static final List<Pattern> AMOUNT_PATTERNS = List.of(
        // Pattern 1: fee keyword followed by amount
        Pattern.compile(
            "(?:contract\\s*(?:value|amount|fee|price)|" +
            "annual\\s*(?:fee|value|amount|subscription|platform|service|license)|" +
            "total\\s*(?:value|consideration|fee|payable)|" +
            "(?:service|subscription|platform|license|placement|software|access)\\s*fee|" +
            "consideration|payable\\s*amount|charges)" +
            "[^\\d₹Rs\\.\\n]{0,30}[₹]?\\s*(?:INR|Rs\\.?)?\\s*([\\d,]+(?:\\.\\d{1,2})?)",
            Pattern.CASE_INSENSITIVE),

        // Pattern 2: currency prefix + number
        Pattern.compile(
            "(?:₹|INR|Rs\\.?)\\s*([1-9][\\d,]+(?:\\.\\d{1,2})?)",
            Pattern.CASE_INSENSITIVE),

        // Pattern 3: number followed by /- or per annum
        Pattern.compile(
            "([1-9][\\d,]{4,})(?:\\.\\d{1,2})?\\s*(?:/-|/\\-|per\\s*(?:annum|year|p\\.a\\.))",
            Pattern.CASE_INSENSITIVE),

        // Pattern 4: fee label then number on next line (multiline contracts)
        Pattern.compile(
            "(?:fee|amount|charges|consideration)\\s*[:\\-]?\\s*\\n+\\s*(?:₹|INR|Rs\\.?)?\\s*([1-9][\\d,]+(?:\\.\\d{1,2})?)",
            Pattern.CASE_INSENSITIVE)
    );

    private static String extractAmount(String text) {
        for (Pattern p : AMOUNT_PATTERNS) {
            Matcher m = p.matcher(text);
            if (m.find()) {
                String raw = m.group(1).replaceAll(",", "").trim();
                try {
                    long val = Long.parseLong(raw.split("\\.")[0]);
                    if (val >= 10_000 && val <= 100_000_000) return String.valueOf(val);
                } catch (NumberFormatException ignored) {}
            }
        }
        return null;
    }

    // ── Date Extraction ───────────────────────────────────────────────

    private static final Pattern DATE_PATTERN = Pattern.compile(
        "\\b(\\d{1,2}[/\\-.]\\d{1,2}[/\\-.]\\d{2,4}|" +
        "\\d{1,2}\\s+(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*[,\\s]+\\d{4}|" +
        "\\d{4}[/\\-]\\d{2}[/\\-]\\d{2})\\b",
        Pattern.CASE_INSENSITIVE);

    private static final List<Pattern> FROM_LABELS = List.of(
        Pattern.compile("(?:effective|commencement|start|valid\\s*from|from\\s*date|w\\.?e\\.?f\\.?)\\s*[:\\-]?\\s*(" + DATE_PATTERN.pattern() + ")", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?:dated|date)\\s*[:\\-]?\\s*(" + DATE_PATTERN.pattern() + ")", Pattern.CASE_INSENSITIVE)
    );

    private static final List<Pattern> TO_LABELS = List.of(
        Pattern.compile("(?:expiry|expiration|end|valid\\s*(?:to|till|until)|to\\s*date)\\s*[:\\-]?\\s*(" + DATE_PATTERN.pattern() + ")", Pattern.CASE_INSENSITIVE)
    );

    private static String[] extractDates(String text) {
        String from = null, to = null;
        for (Pattern p : FROM_LABELS) { Matcher m = p.matcher(text); if (m.find()) { from = toIsoDate(m.group(1)); break; } }
        for (Pattern p : TO_LABELS)   { Matcher m = p.matcher(text); if (m.find()) { to   = toIsoDate(m.group(1)); break; } }
        if (from == null || to == null) {
            List<String> allDates = new ArrayList<>();
            Matcher m = DATE_PATTERN.matcher(text);
            while (m.find() && allDates.size() < 5) {
                String iso = toIsoDate(m.group());
                if (iso != null && !allDates.contains(iso)) allDates.add(iso);
            }
            if (!allDates.isEmpty() && from == null) from = allDates.get(0);
            if (allDates.size() > 1 && to == null)   to   = allDates.get(1);
        }
        return new String[]{ from, to };
    }

    private static String toIsoDate(String raw) {
        if (raw == null) return null;
        raw = raw.trim();
        try {
            if (raw.matches("\\d{4}[-/]\\d{2}[-/]\\d{2}")) return raw.replace("/", "-");
            if (raw.matches("\\d{1,2}[/\\-.]\\d{1,2}[/\\-.]\\d{4}") ||
                raw.matches("\\d{1,2}[/\\-.] \\d{1,2}[/\\-.] \\d{4}")) {
                String[] parts = raw.split("[/\\-.]");
                if (parts.length == 3) {
                    int day   = Integer.parseInt(parts[0].trim());
                    int month = Integer.parseInt(parts[1].trim());
                    int year  = Integer.parseInt(parts[2].trim());
                    if (month > 12) { int tmp = day; day = month; month = tmp; }
                    return String.format("%04d-%02d-%02d", year, month, day);
                }
            }
            if (raw.matches("\\d{1,2}[/\\-.]\\d{1,2}[/\\-.]\\d{2}")) {
                String[] parts = raw.split("[/\\-.]");
                int year = Integer.parseInt(parts[2].trim());
                year += (year < 50 ? 2000 : 1900);
                return String.format("%04d-%02d-%02d", year,
                        Integer.parseInt(parts[1].trim()), Integer.parseInt(parts[0].trim()));
            }
            java.time.format.DateTimeFormatter[] fmts = {
                java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy",    Locale.ENGLISH),
                java.time.format.DateTimeFormatter.ofPattern("d MMMM yyyy",   Locale.ENGLISH),
                java.time.format.DateTimeFormatter.ofPattern("dd MMM, yyyy",  Locale.ENGLISH),
                java.time.format.DateTimeFormatter.ofPattern("dd MMMM, yyyy", Locale.ENGLISH),
            };
            for (var fmt : fmts) {
                try { return java.time.LocalDate.parse(raw, fmt)
                        .format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE); }
                catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
        return null;
    }

    // ── Email Extraction ──────────────────────────────────────────────

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}");

    private static String extractEmail(String text) {
        String[] lines = text.split("\\n");
        for (String line : lines) {
            if (line.toLowerCase().contains("tpo") ||
                line.toLowerCase().contains("placement") ||
                line.toLowerCase().contains("training")) {
                Matcher m = EMAIL_PATTERN.matcher(line);
                if (m.find()) return m.group().toLowerCase();
            }
        }
        Matcher m = EMAIL_PATTERN.matcher(text);
        return m.find() ? m.group().toLowerCase() : null;
    }

    // ── TPO Name Extraction ───────────────────────────────────────────

    private static final List<Pattern> TPO_NAME_PATTERNS = List.of(
        Pattern.compile(
            "(?:training\\s*(?:and|&)?\\s*placement\\s*officer|tpo|placement\\s*officer|contact\\s*person)" +
            "\\s*[:\\-]?\\s*(?:Dr\\.?|Mr\\.?|Mrs\\.?|Ms\\.?|Prof\\.?)?\\s*([A-Z][a-z]+(?:\\s+[A-Z][a-z]+)+)",
            Pattern.CASE_INSENSITIVE),
        Pattern.compile(
            "(?:signed\\s*by|name)\\s*[:\\-]?\\s*(?:Dr\\.?|Mr\\.?|Mrs\\.?|Ms\\.?|Prof\\.?)?\\s*([A-Z][a-z]+(?:\\s+[A-Z][a-z]+)+)",
            Pattern.CASE_INSENSITIVE)
    );

    private static String extractTpoName(String text) {
        for (Pattern p : TPO_NAME_PATTERNS) {
            Matcher m = p.matcher(text);
            if (m.find()) return m.group(1).trim();
        }
        return null;
    }
}

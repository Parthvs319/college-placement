package models.services;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.textract.TextractClient;
import software.amazon.awssdk.services.textract.model.*;

import java.util.*;
import java.util.regex.*;

/**
 * Extracts text from contract PDFs and parses contract fields using regex.
 *
 * Strategy:
 *   1. Try Apache PDFBox (free, fast) for digital/text-based PDFs
 *   2. If extracted text is too short (< 100 chars), fall back to AWS Textract (handles scanned PDFs)
 *
 * Extracted fields: contractAmount, validFrom, validTo, tpoEmail, tpoName
 */
public class ContractTextExtractor {

    private static final int MIN_TEXT_LENGTH = 100;

    private static final String ACCESS_KEY = System.getenv().getOrDefault("S3_ACCESS_KEY", "");
    private static final String SECRET_KEY = System.getenv().getOrDefault("S3_SECRET_KEY", "");
    private static final String REGION     = System.getenv().getOrDefault("S3_REGION", "ap-south-1");

    // ── Public API ────────────────────────────────────────────────────

    /**
     * Result of contract analysis.
     */
    public static class ContractExtractResult {
        public String rawText;
        public String contractAmount;    // e.g. "150000"
        public String validFrom;         // ISO date "YYYY-MM-DD" or null
        public String validTo;           // ISO date "YYYY-MM-DD" or null
        public String tpoEmail;
        public String tpoName;
        public String extractionMethod;  // "pdfbox" | "textract" | "none"
        public Map<String, Double> confidence = new HashMap<>();
    }

    /**
     * Extract contract fields from PDF bytes.
     */
    public static ContractExtractResult extract(byte[] pdfBytes) {
        ContractExtractResult result = new ContractExtractResult();

        // Step 1: Try PDFBox
        String text = extractWithPdfBox(pdfBytes);
        if (text != null && text.trim().length() >= MIN_TEXT_LENGTH) {
            result.rawText = text;
            result.extractionMethod = "pdfbox";
        } else {
            // Step 2: Fall back to Textract
            System.out.println("[ContractOCR] PDFBox text too short (" +
                    (text != null ? text.trim().length() : 0) + " chars), trying Textract...");
            String textractText = extractWithTextract(pdfBytes);
            if (textractText != null && !textractText.isBlank()) {
                result.rawText = textractText;
                result.extractionMethod = "textract";
            } else {
                result.rawText = text != null ? text : "";
                result.extractionMethod = "none";
            }
        }

        // Step 3: Parse fields from text
        parseFields(result);
        return result;
    }

    // ── PDFBox Extraction ─────────────────────────────────────────────

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

    // ── AWS Textract Extraction ───────────────────────────────────────

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

    // ── Field Parsing (Regex) ─────────────────────────────────────────

    private static void parseFields(ContractExtractResult r) {
        if (r.rawText == null || r.rawText.isBlank()) return;
        String text = r.rawText;

        r.contractAmount = extractAmount(text);
        r.tpoEmail       = extractEmail(text);
        r.tpoName        = extractTpoName(text);

        String[] dates = extractDates(text);
        r.validFrom = dates[0];
        r.validTo   = dates[1];

        // Rough confidence based on field presence
        r.confidence.put("contractAmount", r.contractAmount != null ? 0.85 : 0.0);
        r.confidence.put("validFrom",      r.validFrom      != null ? 0.80 : 0.0);
        r.confidence.put("validTo",        r.validTo        != null ? 0.80 : 0.0);
        r.confidence.put("tpoEmail",       r.tpoEmail       != null ? 0.95 : 0.0);
        r.confidence.put("tpoName",        r.tpoName        != null ? 0.70 : 0.0);
    }

    // ── Amount Extraction ─────────────────────────────────────────────

    private static final List<Pattern> AMOUNT_PATTERNS = List.of(
        // "Contract Value: ₹1,50,000" or "Contract Amount: Rs. 1,50,000"
        Pattern.compile("(?:contract\\s*(?:value|amount|fee|price)|annual\\s*(?:fee|value|amount)|total\\s*(?:value|consideration|fee))" +
                        "[^\\d₹Rs\\.]*[₹Rs\\.]*[\\s]*([\\d,]+(?:\\.\\d{1,2})?)",
                        Pattern.CASE_INSENSITIVE),
        // "₹ 1,50,000" or "INR 150000" standalone
        Pattern.compile("(?:₹|INR|Rs\\.?)\\s*([\\d,]+(?:\\.\\d{1,2})?)",
                        Pattern.CASE_INSENSITIVE),
        // "1,50,000/- per annum" or "1,50,000 per year"
        Pattern.compile("([\\d,]{5,})(?:\\.\\d{1,2})?\\s*(?:/-|per\\s*(?:annum|year|p\\.a\\.))",
                        Pattern.CASE_INSENSITIVE)
    );

    private static String extractAmount(String text) {
        for (Pattern p : AMOUNT_PATTERNS) {
            Matcher m = p.matcher(text);
            if (m.find()) {
                String raw = m.group(1).replaceAll(",", "").trim();
                try {
                    long val = Long.parseLong(raw.split("\\.")[0]);
                    // Sanity: reasonable contract range ₹10,000 – ₹10,00,00,000
                    if (val >= 10_000 && val <= 100_000_000) {
                        return String.valueOf(val);
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        return null;
    }

    // ── Date Extraction ───────────────────────────────────────────────

    // Patterns for: dd/MM/yyyy, dd-MM-yyyy, dd MMM yyyy, yyyy-MM-dd, dd.MM.yyyy
    private static final Pattern DATE_PATTERN = Pattern.compile(
        "\\b(\\d{1,2}[/\\-.]\\d{1,2}[/\\-.]\\d{2,4}|" +
        "\\d{1,2}\\s+(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*[,\\s]+\\d{4}|" +
        "\\d{4}[/\\-]\\d{2}[/\\-]\\d{2})\\b",
        Pattern.CASE_INSENSITIVE);

    private static final List<Pattern> FROM_LABELS = List.of(
        Pattern.compile("(?:effective|commencement|start|valid\\s*from|from\\s*date|w\\.?e\\.?f\\.?)\\s*[:\\-]?\\s*(" +
            DATE_PATTERN.pattern() + ")", Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?:dated|date)\\s*[:\\-]?\\s*(" + DATE_PATTERN.pattern() + ")", Pattern.CASE_INSENSITIVE)
    );

    private static final List<Pattern> TO_LABELS = List.of(
        Pattern.compile("(?:expiry|expiration|end|valid\\s*(?:to|till|until)|to\\s*date)\\s*[:\\-]?\\s*(" +
            DATE_PATTERN.pattern() + ")", Pattern.CASE_INSENSITIVE)
    );

    private static String[] extractDates(String text) {
        String from = null, to = null;

        for (Pattern p : FROM_LABELS) {
            Matcher m = p.matcher(text);
            if (m.find()) { from = toIsoDate(m.group(1)); break; }
        }
        for (Pattern p : TO_LABELS) {
            Matcher m = p.matcher(text);
            if (m.find()) { to = toIsoDate(m.group(1)); break; }
        }

        // Fallback: grab first two distinct dates if labeled dates not found
        if (from == null || to == null) {
            List<String> allDates = new ArrayList<>();
            Matcher m = DATE_PATTERN.matcher(text);
            while (m.find() && allDates.size() < 5) {
                String iso = toIsoDate(m.group());
                if (iso != null && !allDates.contains(iso)) allDates.add(iso);
            }
            if (!allDates.isEmpty() && from == null) from = allDates.get(0);
            if (allDates.size() > 1 && to == null) to = allDates.get(1);
        }

        return new String[]{ from, to };
    }

    /** Convert varied date formats → "YYYY-MM-DD" */
    private static String toIsoDate(String raw) {
        if (raw == null) return null;
        raw = raw.trim();
        try {
            // yyyy-MM-dd already
            if (raw.matches("\\d{4}[-/]\\d{2}[-/]\\d{2}")) {
                return raw.replace("/", "-");
            }
            // dd/MM/yyyy or dd-MM-yyyy or dd.MM.yyyy
            if (raw.matches("\\d{1,2}[/\\-.] \\d{1,2}[/\\-.] \\d{4}") ||
                raw.matches("\\d{1,2}[/\\-.]\\d{1,2}[/\\-.]\\d{4}")) {
                String[] parts = raw.split("[/\\-.]");
                if (parts.length == 3) {
                    int day = Integer.parseInt(parts[0].trim());
                    int month = Integer.parseInt(parts[1].trim());
                    int year = Integer.parseInt(parts[2].trim());
                    if (month > 12) { int tmp = day; day = month; month = tmp; }  // handle MM/DD
                    return String.format("%04d-%02d-%02d", year, month, day);
                }
            }
            // dd/MM/yy
            if (raw.matches("\\d{1,2}[/\\-.]\\d{1,2}[/\\-.]\\d{2}")) {
                String[] parts = raw.split("[/\\-.]");
                int year = Integer.parseInt(parts[2].trim());
                year += (year < 50 ? 2000 : 1900);
                return String.format("%04d-%02d-%02d", year,
                        Integer.parseInt(parts[1].trim()), Integer.parseInt(parts[0].trim()));
            }
            // dd MMM yyyy  / dd MMMM yyyy
            java.time.format.DateTimeFormatter[] fmts = {
                java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH),
                java.time.format.DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH),
                java.time.format.DateTimeFormatter.ofPattern("dd MMM, yyyy", Locale.ENGLISH),
                java.time.format.DateTimeFormatter.ofPattern("dd MMMM, yyyy", Locale.ENGLISH),
            };
            for (var fmt : fmts) {
                try {
                    return java.time.LocalDate.parse(raw, fmt)
                            .format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE);
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
        return null;
    }

    // ── Email Extraction ──────────────────────────────────────────────

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}");

    private static String extractEmail(String text) {
        // Prefer emails near "TPO", "Placement", "Training" keywords
        String[] lines = text.split("\\n");
        for (String line : lines) {
            if (line.toLowerCase().contains("tpo") ||
                line.toLowerCase().contains("placement") ||
                line.toLowerCase().contains("training")) {
                Matcher m = EMAIL_PATTERN.matcher(line);
                if (m.find()) return m.group().toLowerCase();
            }
        }
        // Any first email found
        Matcher m = EMAIL_PATTERN.matcher(text);
        return m.find() ? m.group().toLowerCase() : null;
    }

    // ── TPO Name Extraction ───────────────────────────────────────────

    private static final List<Pattern> TPO_NAME_PATTERNS = List.of(
        Pattern.compile("(?:training\\s*(?:and|&)?\\s*placement\\s*officer|tpo|placement\\s*officer|contact\\s*person)\\s*[:\\-]?\\s*(?:Dr\\.?|Mr\\.?|Mrs\\.?|Ms\\.?|Prof\\.?)?\\s*([A-Z][a-z]+(?:\\s+[A-Z][a-z]+)+)",
            Pattern.CASE_INSENSITIVE),
        Pattern.compile("(?:signed\\s*by|name)\\s*[:\\-]?\\s*(?:Dr\\.?|Mr\\.?|Mrs\\.?|Ms\\.?|Prof\\.?)?\\s*([A-Z][a-z]+(?:\\s+[A-Z][a-z]+)+)",
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

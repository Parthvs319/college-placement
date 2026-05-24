package models.services;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.textract.TextractClient;
import software.amazon.awssdk.services.textract.model.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * OCR Service using AWS Textract for document text extraction.
 * Extracts text from PDFs and images stored on S3.
 *
 * This is FREE infrastructure — runs automatically on every document upload.
 * The extracted text enables search, AI features, and ATS scoring.
 *
 * Configure via environment variables (same AWS credentials as S3):
 *   S3_ACCESS_KEY, S3_SECRET_KEY, S3_REGION, S3_BUCKET_NAME
 *
 * For non-AWS setup (Google Vision, local Tesseract), override extractText().
 */
public class OcrService {

    private static final String ACCESS_KEY = System.getenv().getOrDefault("S3_ACCESS_KEY", "");
    private static final String SECRET_KEY = System.getenv().getOrDefault("S3_SECRET_KEY", "");
    private static final String REGION = System.getenv().getOrDefault("S3_REGION", "ap-south-1");
    private static final String BUCKET_NAME = System.getenv().getOrDefault("S3_BUCKET_NAME", "placement-portal");

    private static TextractClient textractClient;
    private static boolean initialized = false;

    /**
     * Initialize Textract client. Called once at startup (after S3Service.initialize).
     */
    public static void initialize() {
        if (ACCESS_KEY.isEmpty() || SECRET_KEY.isEmpty()) {
            System.out.println("[OCR-DEV] AWS Textract not configured — OCR will return empty text");
            return;
        }

        textractClient = TextractClient.builder()
                .region(Region.of(REGION))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(ACCESS_KEY, SECRET_KEY)))
                .build();

        initialized = true;
        System.out.println("[OCR] AWS Textract initialized, region: " + REGION);
    }

    // ── Text Extraction ──────────────────────────────────────────────

    /**
     * Extract text from a document stored on S3.
     * Supports PDF (single/multi-page) and images (PNG, JPG).
     *
     * @param s3Key the S3 object key (e.g. "resumes/abc-123.pdf")
     * @return extracted text, or empty string if OCR fails / not configured
     */
    public static String extractText(String s3Key) {
        if (!initialized) {
            System.out.println("[OCR-DEV] Simulated OCR for: " + s3Key);
            return simulateOcr(s3Key);
        }

        try {
            // For documents on S3, use S3Object reference
            S3Object s3Object = S3Object.builder()
                    .bucket(BUCKET_NAME)
                    .name(s3Key)
                    .build();

            Document document = Document.builder()
                    .s3Object(s3Object)
                    .build();

            DetectDocumentTextRequest request = DetectDocumentTextRequest.builder()
                    .document(document)
                    .build();

            DetectDocumentTextResponse response = textractClient.detectDocumentText(request);

            // Combine all LINE blocks into text
            String text = response.blocks().stream()
                    .filter(b -> b.blockType() == BlockType.LINE)
                    .map(Block::text)
                    .collect(Collectors.joining("\n"));

            System.out.println("[OCR] Extracted " + text.length() + " chars from " + s3Key);
            return text;

        } catch (Exception e) {
            System.err.println("[OCR] Failed to extract text from " + s3Key + ": " + e.getMessage());
            return "";
        }
    }

    /**
     * Extract text from raw bytes (for documents not yet on S3).
     */
    public static String extractTextFromBytes(byte[] fileBytes) {
        if (!initialized) {
            System.out.println("[OCR-DEV] Simulated OCR for byte array (" + fileBytes.length + " bytes)");
            return "";
        }

        try {
            Document document = Document.builder()
                    .bytes(SdkBytes.fromByteArray(fileBytes))
                    .build();

            DetectDocumentTextRequest request = DetectDocumentTextRequest.builder()
                    .document(document)
                    .build();

            DetectDocumentTextResponse response = textractClient.detectDocumentText(request);

            return response.blocks().stream()
                    .filter(b -> b.blockType() == BlockType.LINE)
                    .map(Block::text)
                    .collect(Collectors.joining("\n"));

        } catch (Exception e) {
            System.err.println("[OCR] Failed to extract text from bytes: " + e.getMessage());
            return "";
        }
    }

    // ── Structured Data Extraction (Premium) ─────────────────────────

    /**
     * Extract structured data (tables, key-value pairs) from a document.
     * This is a PREMIUM feature — used for smart resume parsing.
     */
    public static OcrResult extractStructured(String s3Key) {
        if (!initialized) {
            System.out.println("[OCR-DEV] Simulated structured OCR for: " + s3Key);
            return new OcrResult("", List.of(), List.of());
        }

        try {
            S3Object s3Object = S3Object.builder()
                    .bucket(BUCKET_NAME)
                    .name(s3Key)
                    .build();

            AnalyzeDocumentRequest request = AnalyzeDocumentRequest.builder()
                    .document(Document.builder().s3Object(s3Object).build())
                    .featureTypes(FeatureType.TABLES, FeatureType.FORMS)
                    .build();

            AnalyzeDocumentResponse response = textractClient.analyzeDocument(request);

            String fullText = response.blocks().stream()
                    .filter(b -> b.blockType() == BlockType.LINE)
                    .map(Block::text)
                    .collect(Collectors.joining("\n"));

            // Extract key-value pairs
            List<String> keyValues = response.blocks().stream()
                    .filter(b -> b.blockType() == BlockType.KEY_VALUE_SET)
                    .map(Block::text)
                    .filter(t -> t != null)
                    .collect(Collectors.toList());

            // Extract table cells
            List<String> tableCells = response.blocks().stream()
                    .filter(b -> b.blockType() == BlockType.CELL)
                    .map(Block::text)
                    .filter(t -> t != null)
                    .collect(Collectors.toList());

            System.out.println("[OCR] Structured extraction: " + fullText.length() + " chars, "
                    + keyValues.size() + " KV pairs, " + tableCells.size() + " cells from " + s3Key);

            return new OcrResult(fullText, keyValues, tableCells);

        } catch (Exception e) {
            System.err.println("[OCR] Structured extraction failed for " + s3Key + ": " + e.getMessage());
            return new OcrResult("", List.of(), List.of());
        }
    }

    // ── Result Container ─────────────────────────────────────────────

    public static class OcrResult {
        public final String fullText;
        public final List<String> keyValuePairs;
        public final List<String> tableCells;

        public OcrResult(String fullText, List<String> keyValuePairs, List<String> tableCells) {
            this.fullText = fullText;
            this.keyValuePairs = keyValuePairs;
            this.tableCells = tableCells;
        }
    }

    // ── Dev Simulation ───────────────────────────────────────────────

    private static String simulateOcr(String s3Key) {
        // In dev mode, return a placeholder so downstream code doesn't break
        if (s3Key.contains("resumes/")) {
            return "[DEV] Simulated resume text for " + s3Key
                    + "\nName: Test Student\nSkills: Java, Python, SQL\nExperience: 0 years";
        }
        if (s3Key.contains("jd/")) {
            return "[DEV] Simulated JD text for " + s3Key
                    + "\nRole: Software Engineer\nRequired: Java, Spring Boot\nCTC: 8 LPA";
        }
        return "[DEV] Simulated document text for " + s3Key;
    }
}

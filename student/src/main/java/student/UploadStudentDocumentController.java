package student;

import helpers.annotations.StudentRole;
import helpers.customErrors.RoutingError;
import helpers.interfaces.BaseController;
import helpers.utils.ResponseUtils;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.ext.web.RoutingContext;
import models.access.middlewear.student.StudentAccessMiddleware;
import models.body.StudentLoginRequest;
import models.repos.StudentDocumentRepository;
import models.sql.Student;
import models.sql.StudentDocument;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.Set;

/**
 * POST /student/me/documents
 *
 * Body:
 * {
 *   "documentType": "COLLEGE_ID" | "MARKSHEET" | "RESUME" | "AADHAR_CARD" | "PAN_CARD",
 *   "label":        "Semester 5 Marksheet",        // human-readable label
 *   "fileUrl":      "https://s3.../...",            // already uploaded via /api/upload
 *   "fileName":     "marksheet_s5.pdf",
 *   "contentType":  "application/pdf",
 *   "fileSizeBytes": 204800,
 *   "semester":     5                               // only for MARKSHEET
 * }
 *
 * For AADHAR_CARD and PAN_CARD, if the student has aadharNumber/panNumber stored,
 * Gemini vision is called to verify that the document matches. The verification
 * result is stored but TPO can always override.
 *
 * RESUME is auto-verified on upload.
 */
@StudentRole
public enum UploadStudentDocumentController implements BaseController {

    INSTANCE;

    private static final Set<String> VALID_TYPES = Set.of(
            "COLLEGE_ID", "MARKSHEET", "RESUME", "AADHAR_CARD", "PAN_CARD"
    );

    private static final String GEMINI_API_KEY = System.getenv("GEMINI_API_KEY");
    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=";

    @Override
    public void handle(RoutingContext event) {
        StudentAccessMiddleware.INSTANCE.with(event, new ArrayList<>(), this.getClass())
                .map(this::map)
                .subscribe(
                        o -> ResponseUtils.INSTANCE.writeJsonResponse(event, o),
                        error -> ResponseUtils.INSTANCE.handleError(event, error)
                );
    }

    private Object map(StudentLoginRequest request) {
        Student student = request.getStudent();
        var body = request.getRequest();

        String documentType = body.get("documentType");
        if (documentType == null || !VALID_TYPES.contains(documentType.toUpperCase())) {
            throw new RoutingError("documentType must be one of: COLLEGE_ID, MARKSHEET, RESUME, AADHAR_CARD, PAN_CARD");
        }
        documentType = documentType.toUpperCase();

        String fileUrl = body.get("fileUrl");
        if (fileUrl == null || fileUrl.isEmpty()) {
            throw new RoutingError("fileUrl is required. Upload the file first via POST /api/upload?folder=documents");
        }

        String label = body.get("label");
        if (label == null || label.isEmpty()) {
            label = friendlyLabel(documentType, body.isPresent("semester") ? Integer.parseInt(body.get("semester")) : null);
        }

        StudentDocument doc = new StudentDocument();
        doc.student      = student;
        doc.documentType = documentType;
        doc.label        = label;
        doc.fileUrl      = fileUrl;
        doc.fileName     = body.isPresent("fileName")     ? body.get("fileName")     : null;
        doc.contentType  = body.isPresent("contentType")  ? body.get("contentType")  : null;
        doc.fileSizeBytes = body.isPresent("fileSizeBytes") ? Long.parseLong(body.get("fileSizeBytes")) : null;
        doc.semester     = body.isPresent("semester")     ? Integer.parseInt(body.get("semester")) : null;
        doc.verified     = false;
        doc.verificationNote = null;

        // Auto-verify resume on upload
        if ("RESUME".equals(documentType)) {
            doc.verified = true;
            doc.verificationNote = "Auto-verified on upload";
        }

        // AI verification for Aadhar / PAN
        if ("AADHAR_CARD".equals(documentType) && student.getAadharNumber() != null && !student.getAadharNumber().isEmpty()) {
            tryGeminiVerifyId(doc, fileUrl, "Aadhaar", student.getAadharNumber());
        } else if ("PAN_CARD".equals(documentType) && student.getPanNumber() != null && !student.getPanNumber().isEmpty()) {
            tryGeminiVerifyId(doc, fileUrl, "PAN", student.getPanNumber());
        }

        doc.save();
        return StudentDtos.toDocumentDto(doc);
    }

    /**
     * Uses Gemini vision to verify that the document image contains the expected ID number.
     * Sets doc.verified and doc.verificationNote. Never throws — failures are logged only.
     */
    private void tryGeminiVerifyId(StudentDocument doc, String imageUrl, String idType, String expectedNumber) {
        if (GEMINI_API_KEY == null || GEMINI_API_KEY.isEmpty()) {
            doc.verificationNote = "AI verification skipped — GEMINI_API_KEY not configured. Pending TPO review.";
            return;
        }

        try {
            String prompt = "You are an ID verification system. "
                    + "Look at this image which is supposed to be a " + idType + " card. "
                    + "Extract the " + idType + " number from the card. "
                    + "The expected number on file is: \"" + expectedNumber + "\". "
                    + "Return ONLY a JSON object with two keys:\n"
                    + "- \"extracted\": the number you read from the card (string, or null if unreadable)\n"
                    + "- \"matches\": true if the extracted number matches the expected number (ignore spaces/hyphens), false otherwise\n"
                    + "Return ONLY JSON, no explanation.";

            // Determine media type for inline image or URL
            JsonObject content;
            if (imageUrl.startsWith("http")) {
                // Use fileData with the URL
                content = new JsonObject()
                        .put("parts", new JsonArray()
                                .add(new JsonObject().put("text", prompt))
                                .add(new JsonObject()
                                        .put("fileData", new JsonObject()
                                                .put("fileUri", imageUrl)
                                                .put("mimeType", "image/jpeg"))));
            } else {
                content = new JsonObject()
                        .put("parts", new JsonArray()
                                .add(new JsonObject().put("text", prompt)));
            }

            JsonObject body = new JsonObject()
                    .put("contents", new JsonArray().add(content))
                    .put("generationConfig", new JsonObject()
                            .put("temperature", 0.0)
                            .put("maxOutputTokens", 200)
                            .put("responseMimeType", "application/json"));

            URL url = new URL(GEMINI_URL + GEMINI_API_KEY);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.toString().getBytes(StandardCharsets.UTF_8));
            }

            int status = conn.getResponseCode();
            String responseBody;
            try (Scanner scanner = new Scanner(
                    status >= 200 && status < 300 ? conn.getInputStream() : conn.getErrorStream(),
                    StandardCharsets.UTF_8)) {
                responseBody = scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";
            }
            conn.disconnect();

            if (status >= 200 && status < 300) {
                JsonObject geminiResponse = new JsonObject(responseBody);
                String jsonText = geminiResponse
                        .getJsonArray("candidates").getJsonObject(0)
                        .getJsonObject("content").getJsonArray("parts").getJsonObject(0)
                        .getString("text");
                jsonText = jsonText.trim();
                if (jsonText.startsWith("```")) {
                    jsonText = jsonText.replaceAll("```json\\s*", "").replaceAll("```\\s*$", "").trim();
                }
                JsonObject result = new JsonObject(jsonText);
                boolean matches = result.getBoolean("matches", false);
                String extracted = result.getString("extracted", null);

                if (matches) {
                    doc.verified = true;
                    doc.verificationNote = "AI verified: " + idType + " number matches.";
                } else {
                    doc.verified = false;
                    doc.verificationNote = "AI check: extracted \"" + extracted + "\" — does not match stored number. Pending TPO review.";
                }
            } else {
                doc.verificationNote = "AI verification failed (HTTP " + status + "). Pending TPO review.";
            }

        } catch (Exception e) {
            System.err.println("[DocVerify] Gemini verification failed: " + e.getMessage());
            doc.verificationNote = "AI verification error. Pending TPO review.";
        }
    }

    private String friendlyLabel(String type, Integer semester) {
        return switch (type) {
            case "COLLEGE_ID"  -> "College ID Card";
            case "MARKSHEET"   -> semester != null ? "Semester " + semester + " Marksheet" : "Marksheet";
            case "RESUME"      -> "Resume";
            case "AADHAR_CARD" -> "Aadhaar Card";
            case "PAN_CARD"    -> "PAN Card";
            default            -> type;
        };
    }
}

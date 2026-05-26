package models.services;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * AI Service for premium placement features.
 * Supports OpenAI, Anthropic (Claude), and Google Gemini APIs.
 *
 * Premium Features:
 *   1. ATS Score — score resume against standard ATS criteria
 *   2. ATS Resume Generation — create optimized resume from profile data
 *   3. JD-Profile Matching — match score + gap analysis
 *   4. Resume Improvement — suggest changes for a target role
 *   5. Smart Resume Parse — extract structured data from resume into profile fields
 *
 * Configure via environment variables:
 *   AI_PROVIDER  — "gemini" (default), "openai", or "anthropic"
 *   AI_API_KEY   — API key for the provider
 *   AI_MODEL     — model name (default: gemini-2.0-flash / gpt-4o-mini / claude-sonnet-4-20250514)
 */
public class AIService {

    private static final String PROVIDER = System.getenv().getOrDefault("AI_PROVIDER", "gemini");
    private static final String API_KEY = System.getenv().getOrDefault("AI_API_KEY", "");
    private static final String MODEL;

    static {
        String envModel = System.getenv("AI_MODEL");
        if (envModel != null && !envModel.isEmpty()) {
            MODEL = envModel;
        } else {
            switch (PROVIDER.toLowerCase()) {
                case "anthropic": MODEL = "claude-sonnet-4-20250514"; break;
                case "openai":    MODEL = "gpt-4o-mini"; break;
                default:          MODEL = "gemini-2.0-flash"; break;
            }
        }
    }

    private static boolean initialized = false;

    public static void initialize() {
        if (API_KEY.isEmpty()) {
            System.out.println("[AI-DEV] AI API key not configured — AI features will return mock data");
            return;
        }
        initialized = true;
        System.out.println("[AI] Initialized with provider: " + PROVIDER + ", model: " + MODEL);
    }

    // ── 1. ATS Score ─────────────────────────────────────────────────

    /**
     * Calculate ATS (Applicant Tracking System) score for a resume.
     * Returns a JSON with score (0-100) and detailed breakdown.
     */
    public static JsonObject calculateAtsScore(String resumeText) {
        String prompt = "You are an ATS (Applicant Tracking System) scoring engine. "
                + "Analyze the following resume text and return a JSON object with:\n"
                + "- \"score\": overall ATS score (0-100)\n"
                + "- \"formatting\": score (0-20) for formatting clarity\n"
                + "- \"keywords\": score (0-20) for relevant industry keywords\n"
                + "- \"sections\": score (0-20) for proper sections (education, experience, skills, projects)\n"
                + "- \"quantification\": score (0-20) for quantified achievements\n"
                + "- \"readability\": score (0-20) for clean, parseable text\n"
                + "- \"strengths\": array of 3 strongest points\n"
                + "- \"weaknesses\": array of 3 areas to improve\n"
                + "- \"missingSections\": array of missing standard resume sections\n\n"
                + "Return ONLY valid JSON, no explanation.\n\n"
                + "Resume:\n" + truncate(resumeText, 4000);

        return callAI(prompt);
    }

    // ── 2. JD-Profile Match ──────────────────────────────────────────

    /**
     * Match a student's profile/resume against a job description.
     * Returns match percentage and gap analysis.
     */
    public static JsonObject matchJdProfile(String resumeText, String jobDescription) {
        String prompt = "You are a recruitment matching engine. "
                + "Compare the resume against the job description and return a JSON object with:\n"
                + "- \"matchPercentage\": overall match (0-100)\n"
                + "- \"skillMatch\": percentage of required skills the candidate has\n"
                + "- \"experienceMatch\": how well experience aligns (0-100)\n"
                + "- \"matchedSkills\": array of skills the candidate has that match the JD\n"
                + "- \"missingSkills\": array of required skills the candidate lacks\n"
                + "- \"bonusSkills\": array of candidate skills that are nice-to-have for this role\n"
                + "- \"fitSummary\": 2-3 sentence summary of the candidate's fit\n"
                + "- \"improvementAreas\": array of 3 specific things to improve for this role\n\n"
                + "Return ONLY valid JSON, no explanation.\n\n"
                + "Job Description:\n" + truncate(jobDescription, 2000) + "\n\n"
                + "Resume:\n" + truncate(resumeText, 3000);

        return callAI(prompt);
    }

    // ── 3. Resume Improvement Suggestions ────────────────────────────

    /**
     * Suggest improvements to a resume for a target role.
     */
    public static JsonObject suggestImprovements(String resumeText, String targetRole) {
        String prompt = "You are a career coach and resume expert. "
                + "The student is targeting the role: \"" + targetRole + "\". "
                + "Analyze their resume and return a JSON object with:\n"
                + "- \"overallAdvice\": 2-3 sentence high-level advice\n"
                + "- \"suggestions\": array of objects, each with:\n"
                + "    - \"section\": which resume section (e.g. \"Skills\", \"Experience\", \"Projects\")\n"
                + "    - \"issue\": what's wrong or missing\n"
                + "    - \"fix\": specific actionable fix\n"
                + "    - \"priority\": \"HIGH\", \"MEDIUM\", or \"LOW\"\n"
                + "- \"keywordsToAdd\": array of industry keywords to include\n"
                + "- \"projectIdeas\": array of 2-3 project ideas that would strengthen the resume\n"
                + "- \"certificationSuggestions\": array of certifications that would help\n\n"
                + "Return ONLY valid JSON, no explanation.\n\n"
                + "Resume:\n" + truncate(resumeText, 4000);

        return callAI(prompt);
    }

    // ── 4. ATS-Friendly Resume Generation ────────────────────────────

    /**
     * Generate an ATS-optimized resume from student profile data.
     * Returns structured resume content ready for PDF generation.
     */
    public static JsonObject generateAtsResume(JsonObject profileData) {
        String prompt = "You are a professional resume writer specializing in ATS-optimized resumes. "
                + "Using the student's profile data below, generate an ATS-friendly resume. "
                + "Return a JSON object with:\n"
                + "- \"summary\": professional summary (2-3 sentences)\n"
                + "- \"education\": array of education entries, each with degree, institution, year, gpa\n"
                + "- \"skills\": object with \"technical\" (array) and \"soft\" (array)\n"
                + "- \"experience\": array of experience entries (if any), each with title, company, duration, bullets[]\n"
                + "- \"projects\": array of project entries, each with name, description, technologies[], bullets[]\n"
                + "- \"certifications\": array of certification strings\n"
                + "- \"achievements\": array of achievement strings\n"
                + "- \"atsScore\": estimated ATS score (0-100)\n"
                + "- \"tips\": array of 3 tips for further improvement\n\n"
                + "Make bullets action-oriented and quantified where possible.\n"
                + "Return ONLY valid JSON, no explanation.\n\n"
                + "Student Profile:\n" + profileData.encodePrettily();

        return callAI(prompt);
    }

    // ── 5. Smart Resume Parse ────────────────────────────────────────

    /**
     * Parse resume text into structured profile fields.
     * Used to auto-fill student profile from uploaded resume.
     */
    public static JsonObject parseResumeToProfile(String resumeText) {
        String prompt = "You are a resume parsing engine. "
                + "Extract structured data from the resume text and return a JSON object with:\n"
                + "- \"name\": full name\n"
                + "- \"email\": email address (or null)\n"
                + "- \"mobile\": phone number (or null)\n"
                + "- \"skills\": array of technical skills\n"
                + "- \"certifications\": array of certifications\n"
                + "- \"linkedinUrl\": LinkedIn URL (or null)\n"
                + "- \"githubUrl\": GitHub URL (or null)\n"
                + "- \"portfolioUrl\": portfolio URL (or null)\n"
                + "- \"education\": array of objects with degree, institution, year, percentage/cgpa\n"
                + "- \"experience\": array of objects with title, company, duration\n"
                + "- \"projects\": array of objects with name, description, technologies[]\n"
                + "- \"summary\": extracted summary/objective (or null)\n\n"
                + "Return ONLY valid JSON, no explanation. Use null for missing fields.\n\n"
                + "Resume:\n" + truncate(resumeText, 5000);

        return callAI(prompt);
    }

    // ── API Calling ──────────────────────────────────────────────────

    private static JsonObject callAI(String prompt) {
        if (!initialized) {
            System.out.println("[AI-DEV] Mock AI call: " + prompt.substring(0, Math.min(100, prompt.length())) + "...");
            return mockResponse();
        }

        try {
            if ("anthropic".equalsIgnoreCase(PROVIDER)) {
                return callAnthropic(prompt);
            } else if ("gemini".equalsIgnoreCase(PROVIDER)) {
                return callGemini(prompt);
            } else {
                return callOpenAI(prompt);
            }
        } catch (Exception e) {
            System.err.println("[AI] API call failed: " + e.getMessage());
            return new JsonObject().put("error", "AI service unavailable: " + e.getMessage());
        }
    }

    private static JsonObject callOpenAI(String prompt) throws Exception {
        URL url = new URL("https://api.openai.com/v1/chat/completions");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
        conn.setDoOutput(true);

        JsonObject body = new JsonObject()
                .put("model", MODEL)
                .put("response_format", new JsonObject().put("type", "json_object"))
                .put("messages", new JsonArray()
                        .add(new JsonObject()
                                .put("role", "system")
                                .put("content", "You are a JSON API. Always return valid JSON only."))
                        .add(new JsonObject()
                                .put("role", "user")
                                .put("content", prompt)))
                .put("temperature", 0.3)
                .put("max_tokens", 2000);

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
            JsonObject response = new JsonObject(responseBody);
            String content = response.getJsonArray("choices")
                    .getJsonObject(0)
                    .getJsonObject("message")
                    .getString("content");
            return new JsonObject(content);
        } else {
            throw new RuntimeException("OpenAI API error " + status + ": " + responseBody);
        }
    }

    private static JsonObject callAnthropic(String prompt) throws Exception {
        URL url = new URL("https://api.anthropic.com/v1/messages");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("x-api-key", API_KEY);
        conn.setRequestProperty("anthropic-version", "2023-06-01");
        conn.setDoOutput(true);

        JsonObject body = new JsonObject()
                .put("model", MODEL)
                .put("max_tokens", 2000)
                .put("messages", new JsonArray()
                        .add(new JsonObject()
                                .put("role", "user")
                                .put("content", prompt)));

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
            JsonObject response = new JsonObject(responseBody);
            String content = response.getJsonArray("content")
                    .getJsonObject(0)
                    .getString("text");
            // Extract JSON from content (in case wrapped in markdown)
            content = content.trim();
            if (content.startsWith("```")) {
                content = content.replaceAll("```json\\s*", "").replaceAll("```\\s*$", "").trim();
            }
            return new JsonObject(content);
        } else {
            throw new RuntimeException("Anthropic API error " + status + ": " + responseBody);
        }
    }

    private static JsonObject callGemini(String prompt) throws Exception {
        String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/"
                + MODEL + ":generateContent?key=" + API_KEY;

        URL url = new URL(apiUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        JsonObject body = new JsonObject()
                .put("contents", new JsonArray()
                        .add(new JsonObject()
                                .put("parts", new JsonArray()
                                        .add(new JsonObject()
                                                .put("text", "You are a JSON API. Always return valid JSON only, no markdown.\n\n" + prompt)))))
                .put("generationConfig", new JsonObject()
                        .put("temperature", 0.1)
                        .put("maxOutputTokens", 2000)
                        .put("responseMimeType", "application/json"));

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
            JsonObject response = new JsonObject(responseBody);
            String content = response.getJsonArray("candidates")
                    .getJsonObject(0)
                    .getJsonObject("content")
                    .getJsonArray("parts")
                    .getJsonObject(0)
                    .getString("text");
            content = content.trim();
            if (content.startsWith("```")) {
                content = content.replaceAll("```json\\s*", "").replaceAll("```\\s*$", "").trim();
            }
            return new JsonObject(content);
        } else {
            throw new RuntimeException("Gemini API error " + status + ": " + responseBody);
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private static String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }

    private static JsonObject mockResponse() {
        return new JsonObject()
                .put("score", 72)
                .put("matchPercentage", 68)
                .put("message", "AI features require AI_API_KEY to be configured")
                .put("mock", true);
    }
}

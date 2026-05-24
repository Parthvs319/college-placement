package models.services;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URI;
import java.time.Duration;
import java.util.UUID;

/**
 * S3 file storage service for the placement platform.
 *
 * Handles uploads for: resumes, JDs, offer letters, college documents.
 * Supports any S3-compatible storage (AWS S3, MinIO, DigitalOcean Spaces, Cloudflare R2).
 *
 * Configure via environment variables:
 * - S3_ACCESS_KEY, S3_SECRET_KEY
 * - S3_BUCKET_NAME
 * - S3_REGION (default: ap-south-1)
 * - S3_ENDPOINT (optional — for MinIO/R2/Spaces)
 */
public class S3Service {

    private static final String ACCESS_KEY = System.getenv().getOrDefault("S3_ACCESS_KEY", "");
    private static final String SECRET_KEY = System.getenv().getOrDefault("S3_SECRET_KEY", "");
    private static final String BUCKET_NAME = System.getenv().getOrDefault("S3_BUCKET_NAME", "placement-portal");
    private static final String REGION = System.getenv().getOrDefault("S3_REGION", "ap-south-1");
    private static final String ENDPOINT = System.getenv().getOrDefault("S3_ENDPOINT", "");
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB

    private static S3Client s3Client;
    private static S3Presigner presigner;
    private static boolean initialized = false;

    /**
     * Initialize S3 client. Call once at app startup.
     */
    public static void initialize() {
        if (ACCESS_KEY.isEmpty() || SECRET_KEY.isEmpty()) {
            System.out.println("[S3-DEV] S3 not configured — file uploads will be simulated");
            return;
        }

        var credentialsProvider = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(ACCESS_KEY, SECRET_KEY));

        var clientBuilder = S3Client.builder()
                .region(Region.of(REGION))
                .credentialsProvider(credentialsProvider);

        var presignerBuilder = S3Presigner.builder()
                .region(Region.of(REGION))
                .credentialsProvider(credentialsProvider);

        if (!ENDPOINT.isEmpty()) {
            URI endpointUri = URI.create(ENDPOINT);
            clientBuilder.endpointOverride(endpointUri);
            presignerBuilder.endpointOverride(endpointUri);
        }

        s3Client = clientBuilder.build();
        presigner = presignerBuilder.build();
        initialized = true;
        System.out.println("[S3] Initialized — bucket: " + BUCKET_NAME + ", region: " + REGION);
    }

    // ── File Upload ──────────────────────────────────────────────────

    /**
     * Upload a file to S3. Returns the S3 object key.
     *
     * @param folder   subfolder (e.g. "resumes", "jd", "offers", "documents")
     * @param fileName original file name
     * @param data     file bytes
     * @param contentType  MIME type (e.g. "application/pdf")
     * @return the S3 key for the uploaded file
     */
    public static String upload(String folder, String fileName, byte[] data, String contentType) {
        if (data.length > MAX_FILE_SIZE) {
            throw new RuntimeException("File too large. Maximum size is 10 MB.");
        }

        String extension = getExtension(fileName);
        String key = folder + "/" + UUID.randomUUID() + extension;

        if (!initialized) {
            System.out.println("[S3-DEV] Simulated upload: " + key + " (" + data.length + " bytes)");
            return key;
        }

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(BUCKET_NAME)
                .key(key)
                .contentType(contentType)
                .build();

        s3Client.putObject(request, RequestBody.fromBytes(data));
        System.out.println("[S3] Uploaded: " + key + " (" + data.length + " bytes)");
        return key;
    }

    /**
     * Upload with auto-detected content type.
     */
    public static String upload(String folder, String fileName, byte[] data) {
        return upload(folder, fileName, data, detectContentType(fileName));
    }

    // ── Pre-signed URLs ──────────────────────────────────────────────

    /**
     * Generate a pre-signed download URL (valid for 1 hour).
     */
    public static String getDownloadUrl(String key) {
        if (!initialized) {
            return "https://" + BUCKET_NAME + ".s3." + REGION + ".amazonaws.com/" + key;
        }

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofHours(1))
                .getObjectRequest(b -> b.bucket(BUCKET_NAME).key(key))
                .build();

        return presigner.presignGetObject(presignRequest).url().toString();
    }

    /**
     * Generate a pre-signed upload URL for direct browser upload (valid for 15 min).
     * Returns the URL that the client can PUT to directly.
     */
    public static String getUploadUrl(String folder, String fileName, String contentType) {
        String extension = getExtension(fileName);
        String key = folder + "/" + UUID.randomUUID() + extension;

        if (!initialized) {
            return "https://" + BUCKET_NAME + ".s3." + REGION + ".amazonaws.com/" + key;
        }

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(15))
                .putObjectRequest(b -> b.bucket(BUCKET_NAME).key(key).contentType(contentType))
                .build();

        return presigner.presignPutObject(presignRequest).url().toString();
    }

    // ── Download & Delete ────────────────────────────────────────────

    /**
     * Download file bytes from S3.
     */
    public static byte[] download(String key) {
        if (!initialized) {
            System.out.println("[S3-DEV] Simulated download: " + key);
            return new byte[0];
        }

        return s3Client.getObjectAsBytes(b -> b.bucket(BUCKET_NAME).key(key)).asByteArray();
    }

    /**
     * Delete a file from S3.
     */
    public static void delete(String key) {
        if (!initialized) {
            System.out.println("[S3-DEV] Simulated delete: " + key);
            return;
        }

        s3Client.deleteObject(b -> b.bucket(BUCKET_NAME).key(key));
        System.out.println("[S3] Deleted: " + key);
    }

    // ── Public URL (for non-private files) ───────────────────────────

    /**
     * Get the public URL for a file (only works if bucket has public read).
     * For private files, use getDownloadUrl() instead.
     */
    public static String getPublicUrl(String key) {
        if (!ENDPOINT.isEmpty()) {
            return ENDPOINT + "/" + BUCKET_NAME + "/" + key;
        }
        return "https://" + BUCKET_NAME + ".s3." + REGION + ".amazonaws.com/" + key;
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private static String getExtension(String fileName) {
        if (fileName == null) return "";
        int dot = fileName.lastIndexOf('.');
        return dot >= 0 ? fileName.substring(dot) : "";
    }

    private static String detectContentType(String fileName) {
        if (fileName == null) return "application/octet-stream";
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".doc")) return "application/msword";
        if (lower.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".txt")) return "text/plain";
        if (lower.endsWith(".csv")) return "text/csv";
        return "application/octet-stream";
    }

    public static boolean isAllowedType(String fileName, String... allowedExtensions) {
        String ext = getExtension(fileName).toLowerCase();
        for (String allowed : allowedExtensions) {
            if (ext.equals(allowed.startsWith(".") ? allowed : "." + allowed)) return true;
        }
        return false;
    }
}

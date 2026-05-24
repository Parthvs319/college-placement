package models.services;

import com.rabbitmq.client.*;
import io.vertx.core.json.JsonObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

/**
 * RabbitMQ service for async job processing.
 *
 * Queues:
 *   placement.notifications  — email/WhatsApp dispatch
 *   placement.documents      — S3 upload → OCR → text extraction
 *   placement.ai             — ATS scoring, JD matching, resume generation (premium)
 *   placement.scheduler      — scheduled jobs (offer expiry, deadline close)
 *   placement.applications   — application lifecycle (policy enforcement, status cascades)
 *   placement.dlq            — dead letter queue for failed jobs
 *
 * Configure via environment variables:
 *   RABBITMQ_URL (amqp://user:pass@host:port/vhost)
 *   — or individual: RABBITMQ_HOST, RABBITMQ_PORT, RABBITMQ_USER, RABBITMQ_PASS, RABBITMQ_VHOST
 */
public class RabbitMQService {

    // Queue names
    public static final String Q_NOTIFICATIONS = "placement.notifications";
    public static final String Q_DOCUMENTS = "placement.documents";
    public static final String Q_AI = "placement.ai";
    public static final String Q_SCHEDULER = "placement.scheduler";
    public static final String Q_APPLICATIONS = "placement.applications";
    public static final String Q_DLQ = "placement.dlq";

    // Exchange
    public static final String EXCHANGE = "placement.exchange";

    private static Connection connection;
    private static Channel publishChannel;
    private static boolean initialized = false;

    /**
     * Initialize RabbitMQ connection and declare all queues.
     */
    public static void initialize() {
        String url = System.getenv().getOrDefault("RABBITMQ_URL", "");

        if (url.isEmpty()) {
            String host = System.getenv().getOrDefault("RABBITMQ_HOST", "");
            if (host.isEmpty()) {
                System.out.println("[RMQ-DEV] RabbitMQ not configured — jobs will run synchronously");
                return;
            }
            String port = System.getenv().getOrDefault("RABBITMQ_PORT", "5672");
            String user = System.getenv().getOrDefault("RABBITMQ_USER", "guest");
            String pass = System.getenv().getOrDefault("RABBITMQ_PASS", "guest");
            String vhost = System.getenv().getOrDefault("RABBITMQ_VHOST", "/");
            url = "amqp://" + user + ":" + pass + "@" + host + ":" + port + "/" + vhost;
        }

        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setUri(url);
            factory.setAutomaticRecoveryEnabled(true);
            factory.setNetworkRecoveryInterval(5000);

            connection = factory.newConnection("placement-app");
            publishChannel = connection.createChannel();

            // Declare exchange
            publishChannel.exchangeDeclare(EXCHANGE, BuiltinExchangeType.DIRECT, true);

            // Declare queues with DLQ routing
            declareQueue(publishChannel, Q_NOTIFICATIONS);
            declareQueue(publishChannel, Q_DOCUMENTS);
            declareQueue(publishChannel, Q_AI);
            declareQueue(publishChannel, Q_SCHEDULER);
            declareQueue(publishChannel, Q_APPLICATIONS);

            // DLQ — no further dead-lettering
            publishChannel.queueDeclare(Q_DLQ, true, false, false, null);
            publishChannel.queueBind(Q_DLQ, EXCHANGE, Q_DLQ);

            initialized = true;
            System.out.println("[RMQ] Connected and queues declared");
        } catch (Exception e) {
            System.err.println("[RMQ] Failed to initialize: " + e.getMessage());
            System.out.println("[RMQ-DEV] Falling back to synchronous mode");
        }
    }

    private static void declareQueue(Channel channel, String queueName) throws IOException {
        var args = new java.util.HashMap<String, Object>();
        args.put("x-dead-letter-exchange", EXCHANGE);
        args.put("x-dead-letter-routing-key", Q_DLQ);
        args.put("x-message-ttl", 86400000L); // 24h TTL for unprocessed messages

        channel.queueDeclare(queueName, true, false, false, args);
        channel.queueBind(queueName, EXCHANGE, queueName);
    }

    // ── Publish ──────────────────────────────────────────────────────

    /**
     * Publish a job to a queue. The payload is a JSON object.
     *
     * @param queue   queue name (use Q_* constants)
     * @param jobType job type identifier (e.g. "DRIVE_ANNOUNCEMENT", "OCR_RESUME", "ATS_SCORE")
     * @param data    job payload as JsonObject
     */
    public static void publish(String queue, String jobType, JsonObject data) {
        JsonObject message = new JsonObject()
                .put("jobType", jobType)
                .put("data", data)
                .put("timestamp", System.currentTimeMillis())
                .put("retryCount", 0);

        if (!initialized) {
            System.out.println("[RMQ-DEV] Would publish to " + queue + ": " + jobType);
            System.out.println("[RMQ-DEV] Payload: " + data.encodePrettily());
            return;
        }

        try {
            AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                    .deliveryMode(2)  // persistent
                    .contentType("application/json")
                    .type(jobType)
                    .build();

            publishChannel.basicPublish(EXCHANGE, queue, props, message.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            System.err.println("[RMQ] Failed to publish to " + queue + ": " + e.getMessage());
        }
    }

    /**
     * Convenience: publish with just an ID reference.
     */
    public static void publish(String queue, String jobType, Long entityId) {
        publish(queue, jobType, new JsonObject().put("id", entityId));
    }

    // ── Consume ──────────────────────────────────────────────────────

    /**
     * Register a consumer for a queue. Each message is parsed as JSON
     * and passed to the handler. Auto-acks on success, nacks (and requeues once) on failure.
     *
     * @param queue      queue name
     * @param prefetch   number of messages to prefetch (concurrency)
     * @param handler    receives the parsed JsonObject message
     */
    public static void consume(String queue, int prefetch, Consumer<JsonObject> handler) {
        if (!initialized) {
            System.out.println("[RMQ-DEV] Would register consumer for " + queue);
            return;
        }

        try {
            Channel consumeChannel = connection.createChannel();
            consumeChannel.basicQos(prefetch);

            consumeChannel.basicConsume(queue, false, new DefaultConsumer(consumeChannel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope,
                                           AMQP.BasicProperties properties, byte[] body) {
                    try {
                        String json = new String(body, StandardCharsets.UTF_8);
                        JsonObject message = new JsonObject(json);

                        handler.accept(message);

                        consumeChannel.basicAck(envelope.getDeliveryTag(), false);
                    } catch (Exception e) {
                        System.err.println("[RMQ] Consumer error on " + queue + ": " + e.getMessage());
                        try {
                            // Check retry count
                            String json = new String(body, StandardCharsets.UTF_8);
                            JsonObject message = new JsonObject(json);
                            int retries = message.getInteger("retryCount", 0);

                            if (retries < 3) {
                                // Requeue with incremented retry
                                consumeChannel.basicNack(envelope.getDeliveryTag(), false, true);
                            } else {
                                // Send to DLQ (don't requeue)
                                consumeChannel.basicNack(envelope.getDeliveryTag(), false, false);
                            }
                        } catch (IOException ioe) {
                            System.err.println("[RMQ] Failed to nack: " + ioe.getMessage());
                        }
                    }
                }
            });

            System.out.println("[RMQ] Consumer registered for " + queue + " (prefetch=" + prefetch + ")");
        } catch (IOException e) {
            System.err.println("[RMQ] Failed to register consumer for " + queue + ": " + e.getMessage());
        }
    }

    // ── Shutdown ─────────────────────────────────────────────────────

    public static void shutdown() {
        try {
            if (publishChannel != null && publishChannel.isOpen()) publishChannel.close();
            if (connection != null && connection.isOpen()) connection.close();
            System.out.println("[RMQ] Shutdown complete");
        } catch (IOException | TimeoutException e) {
            System.err.println("[RMQ] Error during shutdown: " + e.getMessage());
        }
    }

    public static boolean isInitialized() {
        return initialized;
    }
}

package com.college.placement;

import helpers.sql.SqlConfigFactory;
import io.vertx.core.DeploymentOptions;
import io.vertx.rxjava.core.Vertx;
import models.consumers.AIConsumer;
import models.consumers.ApplicationConsumer;
import models.consumers.DocumentConsumer;
import models.consumers.NotificationConsumer;
import models.services.AIService;
import models.services.OcrService;
import models.services.RabbitMQService;
import models.services.S3Service;
import models.seed.LocationSeeder;
import models.services.WhatsAppService;

public class Application {
    public static void main(String[] args) {
        // DB must be ready before HTTP handlers run
        SqlConfigFactory.init();

        // Seed reference data (states, cities) - safe to call multiple times
        LocationSeeder.seed();

        Vertx vertx = Vertx.vertx();

        // Initialize external services
        WhatsAppService.initialize(vertx);
        S3Service.initialize();
        OcrService.initialize();
        AIService.initialize();
        RabbitMQService.initialize();

        // Register queue consumers
        NotificationConsumer.register();
        DocumentConsumer.register();
        ApplicationConsumer.register();
        AIConsumer.register();

        int instances = Integer.parseInt(System.getenv().getOrDefault("VERTICLE_INSTANCES", "1"));
        vertx.deployVerticle(HttpVerticle.class.getName(), new DeploymentOptions().setInstances(instances));

        // Graceful shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down...");
            RabbitMQService.shutdown();
            vertx.close();
        }));
    }
}

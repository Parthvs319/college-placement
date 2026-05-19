package com.college.placement;

import helpers.sql.SqlConfigFactory;
import io.vertx.core.DeploymentOptions;
import io.vertx.rxjava.core.Vertx;
import models.services.WhatsAppService;

public class Application {
    public static void main(String[] args) {
        // DB must be ready before HTTP handlers run
        SqlConfigFactory.init();

        Vertx vertx = Vertx.vertx();
        WhatsAppService.initialize(vertx);

        int instances = Integer.parseInt(System.getenv().getOrDefault("VERTICLE_INSTANCES", "1"));
        vertx.deployVerticle(HttpVerticle.class.getName(), new DeploymentOptions().setInstances(instances));
    }
}
package com.college.placement;

import helpers.sql.SqlConfigFactory;
import io.vertx.core.DeploymentOptions;
import io.vertx.rxjava.core.Vertx;
import models.services.WhatsAppService;

public class Application {
    public static void main(String[] args) {
        int cores = Runtime.getRuntime().availableProcessors();
        Vertx vertx = Vertx.vertx();
        WhatsAppService.initialize(vertx);
        vertx.deployVerticle(HttpVerticle.class.getName(), new DeploymentOptions().setInstances(cores * 2));
        SqlConfigFactory.init();
    }
}
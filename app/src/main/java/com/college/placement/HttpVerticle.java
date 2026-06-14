package com.college.placement;

import auth.AuthRouter;
import college.CollegeRouter;
import company.CompanyRouter;
import superadmin.SuperAdminRouter;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.ext.web.Router;
import io.vertx.rxjava.ext.web.handler.BodyHandler;
import io.vertx.rxjava.ext.web.handler.CorsHandler;
import models.services.SchedulerService;
import student.StudentRouter;
import user.UserRouter;

public class HttpVerticle extends AbstractVerticle {

    @Override
    public void start() throws Exception {
        Vertx rxVertx = Vertx.newInstance(vertx);
        Router router = Router.router(rxVertx);
        System.out.println("Here");
        router.route().handler(
                CorsHandler.create("*")
                        .allowedMethod(HttpMethod.GET)
                        .allowedMethod(HttpMethod.POST)
                        .allowedMethod(HttpMethod.PUT)
                        .allowedMethod(HttpMethod.DELETE)
                        .allowedHeader("Content-Type")
                        .allowedHeader("Authorization")
        );
        System.out.println("Here2");

        router.route().handler(BodyHandler.create().setBodyLimit(10 * 1024 * 1024)); // 10 MB

        System.out.println("Here3");

        router.get("/").handler(ctx -> ctx.response().end("OK"));

        // Generic upload API (S3 + OCR)
        router.post("/api/upload").handler(UploadController.INSTANCE::handle);

        // Public location APIs (no auth)
        router.get("/location/states").handler(LocationController.INSTANCE::getStates);
        router.get("/location/cities").handler(LocationController.INSTANCE::getCities);

        // Core modules
        router.mountSubRouter("/user", UserRouter.INSTANCE.router(rxVertx));
        router.mountSubRouter("/auth", AuthRouter.INSTANCE.router(rxVertx));

        // Placement portals — each module = one portal
        router.mountSubRouter("/college", CollegeRouter.INSTANCE.router(rxVertx));  // TPO/Admin portal
        router.mountSubRouter("/company", CompanyRouter.INSTANCE.router(rxVertx));  // Company HR portal
        router.mountSubRouter("/student", StudentRouter.INSTANCE.router(rxVertx));  // Student portal
        router.mountSubRouter("/admin", SuperAdminRouter.INSTANCE.router(rxVertx));    // Super Admin portal

        HttpServerOptions options = new HttpServerOptions().setCompressionSupported(true);
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));

        rxVertx.createHttpServer(options)
                .requestHandler(router)
                .rxListen(port)
                .subscribe(server -> {
                    System.out.println("College Placement System started on port " + port);
                    // Start scheduled jobs after server is up
                    SchedulerService.start(rxVertx);
                }, throwable -> {
                    System.err.println("Failed to start HTTP Server: " + throwable.getMessage());
                });
    }
}

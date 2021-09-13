package com.github.cheewail.vertx.petstore.verticle;

import com.github.cheewail.vertx.petstore.service.api.PetService;
import com.github.cheewail.vertx.petstore.service.impl.PetServiceImpl;
import com.github.cheewail.vertx.petstore.service.impl.PetServiceMongoImpl;
import io.vertx.config.ConfigRetriever;
import io.vertx.core.AbstractVerticle;
import io.vertx.serviceproxy.ServiceBinder;

public class PetStoreVerticle extends AbstractVerticle {

    @Override
    public void start() {

        ConfigRetriever configRetriever = ConfigRetriever.create(vertx);
        configRetriever.getConfig().onSuccess(config -> {
            PetService service;
            if (config.getBoolean("mongo.enabled", false).equals(true)) {
                service = new PetServiceMongoImpl(vertx, config);
            } else {
                service = new PetServiceImpl();
            }

            new ServiceBinder(vertx)
                    .setAddress("pet.service")
                    .register(PetService.class, service);

            // Health Check
            vertx.eventBus().consumer("health.pet.service").handler(message -> {
                service.healthCheck(result -> {
                    if(result.succeeded()) {
                        message.reply("pong");
                    } else {
                        message.fail(503, "DB not connected.");
                    }
                });
            });
        });
    }

}
